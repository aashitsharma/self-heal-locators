package onemg.analytics.dump.repository;

import onemg.analytics.dump.model.MockDataModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.http.HttpMethod;

import java.util.Optional;

public interface MockDataRepository extends MongoRepository<MockDataModel,String> {
    Optional<MockDataModel> findByUriAndVerticalAndMethod(String uri, String vertical, HttpMethod method);

}
