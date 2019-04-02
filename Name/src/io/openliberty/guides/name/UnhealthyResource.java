package io.openliberty.guides.name;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("unhealthy")
public class UnhealthyResource {
    @POST
    public String unhealthy() {
        NameHealth.setUnhealthy();
        String name = System.getenv("HOSTNAME")!=null ? "Container " + System.getenv("HOSTNAME") : "Application NameService"; 
              
        return name + " is now unhealthy...\n";
    }
}
