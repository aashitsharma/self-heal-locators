package onemg.analytics.dump.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import onemg.analytics.dump.model.TrainingModel;

public interface TrainingModelRepository extends MongoRepository<TrainingModel,String>{
    
}
