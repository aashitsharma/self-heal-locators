package onemg.analytics.dump.repository;


import onemg.analytics.dump.model.DynamicData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface DynamicDataRepository extends MongoRepository<DynamicData, String>{

    // Custom query to find by name in the 'data' field
    @Query("{ 'data.event_name': ?0 }")
    Optional<DynamicData> findByDataName(String name);
}
