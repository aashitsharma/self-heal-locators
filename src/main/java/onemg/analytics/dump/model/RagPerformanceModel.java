package onemg.analytics.dump.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Model to track RAG performance and statistics
 */
@Data
@Document("rag_performance")
public class RagPerformanceModel extends BaseModel {
    
    @Id
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String id;
    
    @Indexed
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String locator;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("original_page_source_length")
    private Integer originalPageSourceLength;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("optimized_context_length")
    private Integer optimizedContextLength;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("compression_ratio")
    private Double compressionRatio;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("chunks_analyzed")
    private Integer chunksAnalyzed;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("relevant_chunks_found")
    private Integer relevantChunksFound;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("processing_time_ms")
    private Long processingTimeMs;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("automation_type")
    private String automationType;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("success")
    private Boolean success;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("error_message")
    private String errorMessage;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("healed_element_id")
    private String healedElementId;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("confidence_score_improvement")
    private Double confidenceScoreImprovement;
}