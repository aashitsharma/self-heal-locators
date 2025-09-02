package onemg.analytics.dump.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import onemg.analytics.dump.model.ImageDataModel;

public interface ImageDataRepository extends MongoRepository<ImageDataModel,String>{

    /**
     * 
     * @param locator : Image Hex Id 
     * @return
     */
    Optional<ImageDataModel> findByImageHexId(String image_hex_string);


     /**
     * 
     * @param locator : Healed Element Id
     * @return
     */
    Optional<ImageDataModel> findByHealedElementId(String healed_element_id);
    
}
