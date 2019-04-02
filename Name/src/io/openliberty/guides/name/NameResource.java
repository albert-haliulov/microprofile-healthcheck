// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
// end::copyright[]
package io.openliberty.guides.name;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@RequestScoped
@Path("/")
public class NameResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getContainerName() {
    	
    	if (NameHealth.isAlive()) {
    		String name = System.getenv("HOSTNAME")!=null ? "container " + System.getenv("HOSTNAME") : "application NameService"; 
       		return Response.ok("Hello! I'm " + name +"\n").build();
    	}else {
    		return Response.status(Response.Status.SERVICE_UNAVAILABLE)
            .entity("ERROR: Service is currently in maintenance.\n").build();
    	}
    }
    
}
