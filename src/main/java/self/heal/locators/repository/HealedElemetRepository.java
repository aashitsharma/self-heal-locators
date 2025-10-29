package self.heal.locators.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import self.heal.locators.model.HealedElement;

public interface HealedElemetRepository extends MongoRepository<HealedElement,String>{

    /**
     * 
     * @param locator : Locator 
     * @return
     */
    Optional<HealedElement> findByLocator(String locator);

    /**
     * 
     * @param score
     * @param pageable
     * @return
     */
    List<HealedElement> findByConfidenceScoreGreaterThanEqualOrderByConfidenceScoreDesc(Double score, Pageable pageable);

    /**
     * 
     * @param locator
     * @param score
     * @return
     */
    Optional<HealedElement> findByLocatorAndConfidenceScoreGreaterThanEqual(String locator, Double score);

       // Returns the element with the highest confidenceScore for given locator
    Optional<HealedElement> findTopByLocatorOrderByConfidenceScoreDesc(String locator);



    
}
