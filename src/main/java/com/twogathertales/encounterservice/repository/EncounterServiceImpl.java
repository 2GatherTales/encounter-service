package com.twogathertales.encounterservice.repository;

import com.twogathertales.encounterservice.model.encounter.Encounter;
import com.twogathertales.encounterservice.service.GenericService;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.List;

@Repository
public class EncounterServiceImpl implements GenericService<Encounter> {

    @PersistenceContext
    private EntityManager em;

    public Encounter start(Long id) {
        return null;
    }

    @Override
    public Iterable<Encounter> findAll() {
        return null;
    }

    @Override
    public Encounter find(Long playerID) {
        TypedQuery query = em.createQuery("select e from Encounter e WHERE e.id ="+ playerID +
                        " AND e.state = 'ongoing'", Encounter.class);
        List<Encounter>  encounters = query.getResultList();
        if(encounters.size() == 0)
            return null;
        return encounters.get(0);
    }

    @Transactional
    public Encounter save(Encounter encounter) {

        // Is new?
        if (encounter.getId() == null) {
            em.persist(encounter);
            return encounter;
        } else {
            return em.merge(encounter);
        }
    }

    @Override
    public void update(Encounter encounter) {

    }

    @Override
    public void delete(Long id) {

    }
}
