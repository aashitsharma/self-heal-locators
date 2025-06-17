package onemg.analytics.dump.repository;

import onemg.analytics.dump.model.MockDataModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Optional;

public interface MockDataRepository extends MongoRepository<MockDataModel,String> {
    /**
     *
     * @param uri
     * @param vertical
     * @param method
     * @return
     */
    Optional<MockDataModel> findByUriAndVerticalAndMethod(String uri, String vertical, String method);

    /**
     *
     * @param vertical
     * @param method
     * @return
     */
    List<MockDataModel> findByVerticalAndMethod(String vertical, String method);

    /**
     *
     * @param vertical
     * @return
     */
    List<MockDataModel> findByVertical(String vertical);

    /**
     *
     * @param id
     * @return
     */
    Optional<MockDataModel> findById(String id);

}
