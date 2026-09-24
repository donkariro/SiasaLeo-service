package com.arriyiaconsulting.siasaleo.service.infrastructure.shared.persistence;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;
import java.util.List;
import com.arriyiaconsulting.siasaleo.service.infrastructure.shared.error.RecordConflictException;
/** Bound native SQL operations; no domain-specific queries or lifecycle decisions. */
@ApplicationScoped
public class NativeQueries {
    @PersistenceContext private EntityManager em;
    private Query query(String sql, Object... args) {
        Query q = em.createNativeQuery(sql);
        for (int i=0;i<args.length;i++) q.setParameter(i+1,args[i]);
        return q;
    }
    @SuppressWarnings("unchecked")
    public List<Object[]> rows(String sql, Object... args) { return query(sql,args).getResultList(); }
    public Object scalar(String sql, Object... args) { return query(sql,args).getSingleResult(); }
    public long count(String sql, Object... args) { return ((Number)scalar(sql,args)).longValue(); }
    public boolean exists(String sql, Object... args) { return count(sql,args)>0; }
    public void execute(String sql, Object... args) {
        try { query(sql,args).executeUpdate(); }
        catch (PersistenceException e) { throw classify(e); }
    }
    public long insert(String sql, Object... args) {
        try { return ((Number)scalar(sql,args)).longValue(); }
        catch (PersistenceException e) { throw classify(e); }
    }
    private RuntimeException classify(PersistenceException error) {
        for (Throwable cause=error;cause!=null;cause=cause.getCause()) {
            if (cause instanceof java.sql.SQLException sql && sql.getSQLState()!=null) {
                String state=sql.getSQLState();
                if(state.startsWith("23") || state.equals("P0001") || state.equals("40001") || state.equals("40P01"))
                    return new RecordConflictException(error);
            }
        }
        return error;
    }
}
