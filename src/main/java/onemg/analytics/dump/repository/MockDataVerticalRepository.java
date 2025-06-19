package onemg.analytics.dump.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import onemg.analytics.dump.model.MockDataVertical;

public interface MockDataVerticalRepository extends MongoRepository<MockDataVertical,String> {

    /**
     * 
     * @param verticalName
     * @return
     */
    Optional<MockDataVertical> findByVerticalName(String verticalName);

    //Add method to fetch all the name present in a list
    /**
     * 
     * @return
     */
    @Query(value = "{}", fields = "{ 'verticalName' : 1 }")
    List<MockDataVertical> fetchAllVerticals();    

    /**
     * @param id
     */
    Optional<MockDataVertical> findById(String id);
}
