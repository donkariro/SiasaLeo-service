package com.arriyiaconsulting.siasaleo.service.domain.governance.shared.boundary;

import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.*;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.*;

/** Do not expose SQL statements or database row contents to clients. */
@Provider
public class GovernanceDatabaseExceptionMapper implements ExceptionMapper<PersistenceException> {
    private static final Logger LOG=Logger.getLogger(GovernanceDatabaseExceptionMapper.class.getName());
    public Response toResponse(PersistenceException error) {
        for(Throwable cause=error;cause!=null;cause=cause.getCause()) {
            if(cause instanceof SQLException sql && sql.getSQLState()!=null) {
                String state=sql.getSQLState();
                if(state.equals("23505")) return response(409,"A record with this identity already exists");
                if(state.startsWith("23") || state.startsWith("22"))
                    return response(400,"Invalid fields, references, publication state or evidence; check the governance API requirements");
            }
        }
        LOG.log(Level.SEVERE,"Database operation failed",error);
        return response(500,"Database operation failed");
    }
    private Response response(int status,String message) { return Response.status(status).entity(Map.of("error",message)).build(); }
}
