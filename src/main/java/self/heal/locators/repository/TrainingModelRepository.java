package self.heal.locators.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import self.heal.locators.model.TrainingModel;

public interface TrainingModelRepository extends MongoRepository<TrainingModel,String>{
    
}
