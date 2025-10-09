package onemg.analytics.dump.repository;

import onemg.analytics.dump.model.RagPerformanceModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for RAG Performance tracking
 */
@Repository
public interface RagPerformanceRepository extends MongoRepository<RagPerformanceModel, String> {
    
    /**
     * Find performance records by locator
     */
    List<RagPerformanceModel> findByLocator(String locator);

    /**
     * Find RAG operations with high compression ratios
     */
    @Query("{'compressionRatio': {$gte: ?0}, 'success': true}")
    List<RagPerformanceModel> findHighCompressionRatioOperations(double minCompressionRatio, PageRequest pageRequest);
    
    /**
     * Get all fields for successful operations for statistics calculation
     */
    @Query("{'success': true}")
    List<RagPerformanceModel> findAllSuccessfulOperationsCompressionRatio();
    
    /**
     * Find operations by automation type
     */
    List<RagPerformanceModel> findByAutomationType(String automationType);
    
}