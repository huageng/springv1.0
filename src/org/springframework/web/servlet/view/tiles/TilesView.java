/*
 * Copyright 2002-2004 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.view.tiles;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.ComponentDefinition;
import org.apache.struts.tiles.Controller;
import org.apache.struts.tiles.DefinitionsFactory;
import org.apache.struts.tiles.TilesUtilImpl;

import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;
import org.springframework.web.servlet.view.InternalResourceView;

/**
 * View implementation that retrieves a Tiles definition.
 * The "url" property is interpreted as name of a Tiles definition.
 *
 * <p>TilesJstlView with JSTL support is a separate class,
 * mainly to avoid JSTL dependencies in this class.
 *
 * <p>Depends on a Tiles DefinitionsFactory which must be available
 * in the ServletContext. This factory is typically set up via a
 * TilesConfigurer bean definition in the application context.
 *
 * <p>A component controller specified in the Tiles definition will receive
 * a reference to the current Spring ApplicationContext if it implements
 * ApplicationContextAware. The ComponentControllerSupport class provides
 * a convenient base class for such Spring-aware component controllers.
 *
 * @author Alef Arendsen
 * @author Juergen Hoeller
 * @see #setUrl
 * @see TilesJstlView
 * @see TilesConfigurer
 * @see ComponentControllerSupport
 * @see ApplicationContextAware
 */
public class TilesView extends InternalResourceView {

	/**
	 * Name of the attribute that will override the path of the layout page
	 * to render. A Tiles component controller can set such an attribute
	 * to dynamically switch the look and feel of a Tiles page.
	 * @see #setPath
	 */
	public static final String PATH_ATTRIBUTE = TilesView.class.getName() + ".PATH";

	/**
	 * Set the path of the layout page to render.
	 * @param request current HTTP request
	 * @param path the path of the layout page
	 * @see #PATH_ATTRIBUTE
	 */
	public static void setPath(HttpServletRequest request, String path) {
		request.setAttribute(PATH_ATTRIBUTE, path);
	}


	private DefinitionsFactory definitionsFactory;

	protected void initApplicationContext() throws ApplicationContextException {
		super.initApplicationContext();

		// get definitions factory
		this.definitionsFactory = (DefinitionsFactory)
		    getWebApplicationContext().getServletContext().getAttribute(TilesUtilImpl.DEFINITIONS_FACTORY);
		if (this.definitionsFactory == null) {
			throw new ApplicationContextException("Tiles definitions factory not found: TilesConfigurer not defined?");
		}
	}

	/**
	 * Prepare for rendering the Tiles definition: Execute the associated
	 * component controller if any, and determine the request dispatcher path.
	 */
	protected String prepareForRendering(HttpServletRequest request, HttpServletResponse response)
	    throws Exception {

		// get component definition
		ComponentDefinition definition = getComponentDefinition(this.definitionsFactory, request);
		if (definition == null) {
			throw new ServletException("No Tiles definition found for name '" + getUrl() + "'");
		}

		// get current component context
		ComponentContext context = getComponentContext(definition, request);

		// execute component controller associated with definition, if any
		Controller controller = getController(definition, request);
		if (controller != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Executing Tiles controller [" + controller + "]");
			}
			executeController(controller, context, request, response);
		}

		// determine the path of the definition
		String path = getDispatcherPath(definition, request);
		if (path == null) {
			throw new ServletException("Could not determine a path for Tiles definition '" +
			                           definition.getName() + "'");
		}

		return path;
	}

	/**
	 * Determine the Tiles component definition for the given Tiles
	 * definitions factory.
	 * @param factory the Tiles definitions factory
	 * @param request current HTTP request
	 * @return the component definition
	 */
	protected ComponentDefinition getComponentDefinition(DefinitionsFactory factory, HttpServletRequest request)
	    throws Exception {
		return factory.getDefinition(getUrl(), request, getServletContext());
	}

	/**
	 * Determine the Tiles component context for the given Tiles definition.
	 * @param definition the Tiles definition to render
	 * @param request current HTTP request
	 * @return the component context
	 * @throws Exception if preparations failed
	 */
	protected ComponentContext getComponentContext(ComponentDefinition definition, HttpServletRequest request)
	    throws Exception {
		ComponentContext context = ComponentContext.getContext(request);
		if (context == null) {
			context = new ComponentContext(definition.getAttributes());
			ComponentContext.setContext(context, request);
		}
		else {
			context.addMissing(definition.getAttributes());
		}
		return context;
	}

	/**
	 * Determine and initialize the Tiles component controller for the
	 * given Tiles definition, if any.
	 * @param definition the Tiles definition to render
	 * @param request current HTTP request
	 * @return the component controller to execute, or null if none
	 * @throws Exception if preparations failed
	 */
	protected Controller getController(ComponentDefinition definition, HttpServletRequest request)
			throws Exception {
		Controller controller = definition.getOrCreateController();
		if (controller instanceof ApplicationContextAware) {
			((ApplicationContextAware) controller).setApplicationContext(getApplicationContext());
		}
		return controller;
	}

	/**
	 * Execute the given Tiles controller.
	 * @param controller the component controller to execute
	 * @param context the component context
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @throws Exception if controller execution failed
	 */
	protected void executeController(Controller controller, ComponentContext context,
	                                 HttpServletRequest request, HttpServletResponse response)
	    throws Exception {
		controller.perform(context, request, response, getServletContext());
	}

	/**
	 * Determine the dispatcher path for the given Tiles definition,
	 * i.e. the request dispatcher path of the layout page.
	 * @param definition the Tiles definition to render
	 * @param request current HTTP request
	 * @return the path of the layout page to render
	 * @throws Exception if preparations failed
	 */
	protected String getDispatcherPath(ComponentDefinition definition, HttpServletRequest request)
	    throws Exception {
		Object pathAttr = request.getAttribute(PATH_ATTRIBUTE);
		return (pathAttr != null ? pathAttr.toString() : definition.getPath());
	}

}
