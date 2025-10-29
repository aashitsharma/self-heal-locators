package self.heal.locators.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Comparator;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.gridfs.model.GridFSFile;

import lombok.Data;
import self.heal.locators.JsonConfig;
import self.heal.locators.config.RagConfiguration;
import self.heal.locators.model.BaseModel;
import self.heal.locators.model.ErrorModel;
import self.heal.locators.model.HealedElement;
import self.heal.locators.model.HealingModel;
import self.heal.locators.model.ImageDataModel;
import self.heal.locators.model.TrainingModel;
import self.heal.locators.repository.HealedElemetRepository;
import self.heal.locators.repository.ImageDataRepository;
import self.heal.locators.repository.TrainingModelRepository;
import self.heal.locators.utils.CommonUtility;
import self.heal.locators.utils.ImageService;
import self.heal.locators.service.RagService;
import self.heal.locators.service.ContextAnalyzerService;
import self.heal.locators.service.LocatorMatcher;
import self.heal.locators.model.RagPerformanceModel;
import self.heal.locators.repository.RagPerformanceRepository;

@RestController
@RequestMapping("/v1/openai/api")
public class OpenAI {
    private final RestTemplate restTemplate = new RestTemplate();
    @Autowired
    HealedElemetRepository healRepo ;
    @Autowired
    ImageDataRepository imageRepo;
    @Autowired
    TrainingModelRepository trainingRepo;
    @Autowired
    RagPerformanceRepository ragPerformanceRepo;
    @Autowired
    RagService ragService;
    @Autowired
    ContextAnalyzerService contextAnalyzerService;
    @Autowired
    private RagConfiguration ragConfig;
    

    private final ImageService imageService;

    @Autowired
    public OpenAI(ImageService imageService) {
        this.imageService = imageService;
    }

    private static final Logger LOGGER = Logger.getLogger(OpenAI.class);
static String OPTIMIZED_SYSTEM_PROMPT = 
    "You are a UI automation locator healing expert. Analyze failed locators against page source and provide healing suggestions.\n\n" +
    "CRITICAL RULES FOR TEXT MATCHING:\n" +
    "1. NEVER modify text content in locators unless the exact text is not found in page source\n" +
    "2. PRESERVE EXACT SPACING, capitalization, and special characters in text searches\n" +
    "3. If a locator uses text matching (contains(@text, 'value')), verify the EXACT text exists in page source\n" +
    "4. Case-insensitive searches using translate() function must preserve the original case structure\n" +
    "5. Do NOT remove spaces or modify text formatting unless absolutely necessary for healing\n" +
    "6. If Healing is required pick up the EXACT TEXT PRESENT IN PAGE SOURCE\n\n"+
    "CRITICAL OR CONDITION RULE:\n" +
    "- If a locator contains OR conditions (using '|' or 'or'), and ANY ONE of those conditions matches an element in the page source, you MUST return the EXACT ORIGINAL locator with approach='already_present' and confidence=1.0\n" +
    "- DO NOT simplify or optimize OR conditions even if only one condition matches\n" +
    "- OR conditions are intentionally designed for fallback scenarios - preserve them completely\n\n" +
    "TEXT VALIDATION PROCESS:\n" +
    "1. Extract text values from locator (e.g., from contains(@text, 'some text'))\n" +
    "2. Search for EXACT text match in page source (case-sensitive)\n" +
    "3. If exact match found → return original locator as 'already_present'\n" +
    "4. If case-insensitive match found → return original locator as 'already_present' (do NOT change case)\n" +
    "5. Only modify text if NO reasonable match exists in page source\n\n" +
    "ANALYSIS PROCESS:\n" +
    "1. Parse the page source (HTML/XML)\n" +
    "2. Check if locator contains OR conditions (look for '|' or 'or')\n" +
    "3. If OR conditions exist, test each condition separately\n" +
    "4. If ANY condition matches → return original locator as 'already_present'\n" +
    "5. For text-based locators, verify exact text exists in page source\n" +
    "6. If no OR conditions match → proceed with healing logic\n" +
    "7. Calculate confidence score (0.0-1.0) based on:\n" +
    "   - Attribute similarity (id, class, text, resource-id)\n" +
    "   - Element hierarchy match\n" +
    "   - Unique identification capability\n\n" +
    "HEALING RULES:\n" +
    "- If locator works exactly as provided → confidence: 1.0, approach: 'already_present'\n" +
    "- If ANY OR condition works → confidence: 1.0, approach: 'already_present', return ORIGINAL locator\n" +
    "- If text exists in page source with different formatting → return ORIGINAL locator as 'already_present'\n" +
    "- If healable with 60%+ similarity → provide healed locator, approach: 'healing'\n" +
    "- If <60% match → return original with approach: 'not_match'\n" +
    "- Always return XPath format\n" +
    "- Avoid encoded characters, coordinates, bounds\n" +
    "- Prefer stable attributes: resource-id, class, hierarchy\n\n";

// Enhanced output format with explicit text handling examples
static String OUTPUT_FORMAT = 
    "RESPOND ONLY WITH VALID JSON:\n" +
    "{\n" +
    "  \"locator\": \"xpath_here\",\n" +
    "  \"approach\": \"already_present|healing|not_match\",\n" +
    "  \"confidence_score\": 0.0\n" +
    "}\n\n" +
    "## CRITICAL OUTPUT RULES:\n\n" +
    "1. ALWAYS return XPath locator - this is non-negotiable\n\n" +
    "2. FOR TEXT-BASED LOCATORS:\n" +
    "   - If locator contains text searches (contains(@text, 'value')), find the EXACT text in page source\n" +
    "   - Do NOT modify spacing, capitalization, or punctuation in text values\n" +
    "   - If text exists with minor formatting differences → return ORIGINAL locator as 'already_present'\n" +
    "   - Only modify text if completely absent from page source\n\n" +
    "3. FOR OR CONDITIONS - If original locator contains 'or' or '|' operators:\n" +
    "   - If ANY condition in the OR statement matches page source → RETURN EXACT ORIGINAL LOCATOR\n" +
    "   - DO NOT remove working OR conditions\n" +
    "   - DO NOT simplify or optimize the locator\n\n" +
    "4. If locator works as-is (including text matching) → return:\n" +
    "{\n" +
    "  \"locator\": \"<exact original locator>\",\n" +
    "  \"approach\": \"already_present\",\n" +
    "  \"confidence_score\": 1.0\n" +
    "}\n\n" +
    "5. If healable with 60-100% match score → return:\n" +
    "{\n" +
    "  \"locator\": \"<corrected healed locator>\",\n" +
    "  \"approach\": \"healing\",\n" +
    "  \"confidence_score\": <decimal between 0.60-1.0>\n" +
    "}\n\n" +
    "6. If match score below 60% → return:\n" +
    "{\n" +
    "  \"locator\": \"<exact original locator>\",\n" +
    "  \"approach\": \"not_match\",\n" +
    "  \"confidence_score\": <decimal below 0.60>\n" +
    "}\n\n";


// Updated examples with specific OR condition scenarios
static String EXAMPLES = 
    "EXAMPLES:\n\n" +
    "Example 1 - OR Condition Match:\n" +
    "Input: Failed: //button[@id='login' or @id='loginBtn'] | Source: <button id='loginBtn'>Login</button>\n" +
    "Output: {\"locator\":\"//button[@id='login' or @id='loginBtn']\",\"approach\":\"already_present\",\"confidence_score\":1.0}\n" +
    "Explanation: Second OR condition matches, return ORIGINAL locator\n\n" +
    "Example 2 - Simple Match:\n" +
    "Input: Failed: //div[@class='header'] | Source: <div class='header'>Title</div>\n" +
    "Output: {\"locator\":\"//div[@class='header']\",\"approach\":\"already_present\",\"confidence_score\":1.0}\n\n" +
    "Example 3 - Healing Required:\n" +
    "Input: Failed: //button[@id='login'] | Source: <button id='loginButton'>Login</button>\n" +
    "Output: {\"locator\":\"//button[@id='loginButton']\",\"approach\":\"healing\",\"confidence_score\":0.95}\n\n" +
    "Example 4 - Complex OR Condition:\n" +
    "Input: Failed: //div[@resource-id='list1' or @resource-id='list2']//span[@text='item'] | Source: <div resource-id='list2'><span text='item'>Content</span></div>\n" +
    "Output: {\"locator\":\"//div[@resource-id='list1' or @resource-id='list2']//span[@text='item']\",\"approach\":\"already_present\",\"confidence_score\":1.0}\n\n"+
    "Example 5 - Text Healing Required:\n" +
    "Input: Failed: //*[@resource-id='com.aranoah.healthkart.plus:id/name' and contains(translate(@text, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'telma-am h 40 tablet')]/..//*[contains(@resource-id,'com.aranoah.healthkart.plus:id/price_container')] | Source: <android.widget.TextView index=\\\"1\\\" package=\\\"com.aranoah.healthkart.plus\\\" class=\\\"android.widget.TextView\\\" text=\\\"Telma 40 Tablet\\\" resource-id=\\\"com.aranoah.healthkart.plus:id/name\\\" checkable=\\\"false\\\" checked=\\\"false\\\" clickable=\\\"false\\\" enabled=\\\"true\\\" focusable=\\\"false\\\" focused=\\\"false\\\" long-clickable=\\\"false\\\" password=\\\"false\\\" scrollable=\\\"false\\\" selected=\\\"false\\\" bounds=\\\"[390,1049][1036,1108]\\\" displayed=\\\"true\\\" />\\n" + //
                "                  <android.widget.TextView index=\\\"2\\\" package=\\\"com.aranoah.healthkart.plus\\\" class=\\\"android.widget.TextView\\\" text=\\\"30 tablets\\\" resource-id=\\\"com.aranoah.healthkart.plus:id/property\\\" checkable=\\\"false\\\" checked=\\\"false\\\" clickable=\\\"false\\\" enabled=\\\"true\\\" focusable=\\\"false\\\" focused=\\\"false\\\" long-clickable=\\\"false\\\" password=\\\"false\\\" scrollable=\\\"false\\\" selected=\\\"false\\\" bounds=\\\"[390,1108][1036,1159]\\\" displayed=\\\"true\\\" />\\n" + //
                "                  <android.widget.TextView index=\\\"3\\\" package=\\\"com.aranoah.healthkart.plus\\\" class=\\\"android.widget.TextView\\\" text=\\\"Get in 30 mins\\\" resource-id=\\\"com.aranoah.healthkart.plus:id/delivery\\\" checkable=\\\"false\\\" checked=\\\"false\\\" clickable=\\\"false\\\" enabled=\\\"true\\\" focusable=\\\"false\\\" focused=\\\"false\\\" long-clickable=\\\"false\\\" password=\\\"false\\\" scrollable=\\\"false\\\" selected=\\\"false\\\" bounds=\\\"[390,1181][653,1232]\\\" displayed=\\\"true\\\" />\\n" + //
                "                  <android.view.ViewGroup index=\\\"4\\\" package=\\\"com.aranoah.healthkart.plus\\\" class=\\\"android.view.ViewGroup\\\" text=\\\"\\\" resource-id=\\\"com.aranoah.healthkart.plus:id/price_container\\\" checkable=\\\"false\\\" checked=\\\"false\\\" clickable=\\\"false\\\" enabled=\\\"true\\\" focusable=\\\"false\\\" focused=\\\"false\\\" long-clickable=\\\"false\\\" password=\\\"false\\\" scrollable=\\\"false\\\" selected=\\\"false\\\" bounds=\\\"[390,1254][1036,1489]\\\" displayed=\\\"true\\\">\n" +
    "Output: {\"locator\":\"//*[@resource-id='com.aranoah.healthkart.plus:id/name' and contains(translate(@text, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'telma40 tablet')]/..//*[contains(@resource-id,'com.aranoah.healthkart.plus:id/price_container')]\",\"approach\":\"healing\",\"confidence_score\":0.95}\n\n"+
    "Example 6 - Complex Case-Insensitive Text:\n" +
    "Input: Failed: //*[contains(translate(@text, 'ABCD', 'abcd'), 'telma 40')] | Source: <TextView text='TELMA 40 Tablet'/>\n" +
    "Output: {\"locator\":\"//*[contains(translate(@text, 'ABCD', 'abcd'), 'telma 40')]\",\"approach\":\"already_present\",\"confidence_score\":1.0}\n\n";
;

    @PostMapping("/get-healed-locator")
    public ResponseEntity<?> getHealedLocator(@RequestBody HealingModel request) {
        if (request.getLocator().replaceAll("\"", "'") == null || request.getPageSource() == null) {
            return new ResponseEntity<>(
                new ErrorModel().errorResp(400, "locator and page_source are mandatory fields"),
                HttpStatusCode.valueOf(HttpStatus.BAD_REQUEST.value()));
        }
        Boolean isPresent = LocatorMatcher.isLocatorPresent(request.getLocator().replaceAll("\"", "'"), request.getPageSource(), ContextAnalyzerService.detectAutomationType(request.getPageSource()));

        if(isPresent){
            String xpathLocator = LocatorMatcher.ensureXPath(request.getLocator().replaceAll("\"", "'"), request.getPageSource());
            return new ResponseEntity<>(locatorPresentInPageSource(request.getLocator().replaceAll("\"", "'"),xpathLocator),HttpStatusCode.valueOf(HttpStatus.OK.value()));
        }
        // Get similar examples for context
        PageRequest topThree = PageRequest.of(0, 3);
        List<HealedElement> examples = healRepo.findByConfidenceScoreGreaterThanEqualOrderByConfidenceScoreDesc(0.80, topThree);
        
        Optional<ImageDataModel> imageModel = imageRepo.findByImageHexId(request.getImageDataId());

        try {
            return callGroqAPI(request, examples, imageModel);
        } catch (Exception e) {
            LOGGER.warn("Groq API failed: " + e.getMessage() + ". Falling back to Gemini.");
            try {
                return callGeminiAPI(request, examples, imageModel);
            } catch (Exception geminiException) {
                LOGGER.error("Both APIs failed", geminiException);
                Map<String,String> resp = new HashMap<>();
                resp.put("healed_locator", request.getLocator().replaceAll("\"", "'"));
                resp.put("message", "AI Service Failed");
                return new ResponseEntity<>(
                    resp,
                    HttpStatusCode.valueOf(HttpStatus.OK.value()));
            }
        }
    }

    private ResponseEntity<?> callGroqAPI(HealingModel request, List<HealedElement> examples, Optional<ImageDataModel> imageModel) {
        long startTime = System.currentTimeMillis();
        RagPerformanceModel ragPerformance = new RagPerformanceModel();
        
        try {
            String baseUrl = JsonConfig.config.getMockObject().get("GROQ_API_URL").toString();
            
            HttpHeaders header = new HttpHeaders();
            header.setAll(setHeaders());
            
            AiRequestBody requestBody = new AiRequestBody();
            requestBody.setModel(JsonConfig.config.getMockObject().get("GROQ_MODEL").toString());
            requestBody.setTemperature(.05);
            requestBody.setMax_tokens(5000);
            
            // Track RAG performance
            ragPerformance.setLocator(request.getLocator().replaceAll("\"", "'"));
            ragPerformance.setOriginalPageSourceLength(request.getPageSource() != null ? request.getPageSource().length() : 0);
            
            // Analyze page context
            ContextAnalyzerService.PageAnalysis pageAnalysis = contextAnalyzerService.analyzePage(request.getPageSource());
            ragPerformance.setAutomationType(pageAnalysis.automationType.toString());
            
            // Build RAG-optimized prompt
            requestBody.setMessage("system", OPTIMIZED_SYSTEM_PROMPT + OUTPUT_FORMAT + EXAMPLES);
            requestBody.setMessage("user", buildUserPrompt(request.getLocator().replaceAll("\"", "'"), request.getPageSource(), examples));
        
            // Get context statistics
            RagService.ContextStats contextStats = ragService.getContextStats(request.getLocator().replaceAll("\"", "'"), request.getPageSource());
            ragPerformance.setOptimizedContextLength(contextStats.optimizedLength);
            ragPerformance.setCompressionRatio(100.0 - contextStats.compressionRatio); // Invert to show compression
            
            HttpEntity<AiRequestBody> entity = new HttpEntity<>(requestBody, header);
            
            ResponseEntity<String> response = restTemplate.exchange(baseUrl, HttpMethod.POST, entity, String.class);
            
            ragPerformance.setSuccess(true);
            ragPerformance.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            
            return processGroqResponse(response, request, imageModel, JsonConfig.config.getMockObject().get("GROQ_MODEL").toString(), ragPerformance);
            
        } catch (Exception e) {
            ragPerformance.setSuccess(false);
            ragPerformance.setErrorMessage(e.getMessage());
            ragPerformance.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            ragPerformanceRepo.save(ragPerformance);
            throw e;
        }
    }

    private ResponseEntity<?> callGeminiAPI(HealingModel request, List<HealedElement> examples, Optional<ImageDataModel> imageModel) {
        long startTime = System.currentTimeMillis();
        RagPerformanceModel ragPerformance = new RagPerformanceModel();
        
        try {
            String baseUrl = JsonConfig.config.getMockObject().get("GEMENI_API_URL").toString();
            
            HttpHeaders header = new HttpHeaders();
            header.setAll(setGeminiHeaders());
            
            // Track RAG performance
            ragPerformance.setLocator(request.getLocator().replaceAll("\"", "'"));
            ragPerformance.setOriginalPageSourceLength(request.getPageSource() != null ? request.getPageSource().length() : 0);
            
            // Analyze page context
            ContextAnalyzerService.PageAnalysis pageAnalysis = contextAnalyzerService.analyzePage(request.getPageSource());
            ragPerformance.setAutomationType(pageAnalysis.automationType.toString());
            
            String systemPrompt = OPTIMIZED_SYSTEM_PROMPT + OUTPUT_FORMAT + EXAMPLES;
            String userPrompt = buildUserPrompt(request.getLocator().replaceAll("\"", "'"), request.getPageSource(), examples);
            
            // Get context statistics
            RagService.ContextStats contextStats = ragService.getContextStats(request.getLocator().replaceAll("\"", "'"), request.getPageSource());
            ragPerformance.setOptimizedContextLength(contextStats.optimizedLength);
            ragPerformance.setCompressionRatio(100.0 - contextStats.compressionRatio);
            
            GeminiRequestBody reqBody = new GeminiRequestBody(systemPrompt, userPrompt);
            
            HttpEntity<GeminiRequestBody> entity = new HttpEntity<>(reqBody, header);
            
            ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "?key=" + JsonConfig.config.getMockObject().get("GEMENI_API_KEY"), 
                HttpMethod.POST, entity, String.class);
            
            ragPerformance.setSuccess(true);
            ragPerformance.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            
            return processGeminiResponse(response, request, imageModel, ragPerformance);
            
        } catch (Exception e) {
            ragPerformance.setSuccess(false);
            ragPerformance.setErrorMessage(e.getMessage());
            ragPerformance.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            ragPerformanceRepo.save(ragPerformance);
            throw e;
        }
    }

    private ResponseEntity<?> processGroqResponse(ResponseEntity<String> response, HealingModel request, 
                                                Optional<ImageDataModel> imageModel, String modelName, RagPerformanceModel ragPerformance) {
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Groq API returned: " + response.getStatusCode());
        }

        JSONObject jsonObject = new JSONObject(response.getBody());
        
        if (!jsonObject.has("choices")) {
            throw new RuntimeException("Invalid Groq response format");
        }

        String content = jsonObject.getJSONArray("choices")
                                 .getJSONObject(0)
                                 .getJSONObject("message")
                                 .getString("content");

        return processAIResponse(content, request, imageModel, modelName, "groq", ragPerformance);
    }

    private ResponseEntity<?> processGeminiResponse(ResponseEntity<String> response, HealingModel request, 
                                                  Optional<ImageDataModel> imageModel, RagPerformanceModel ragPerformance) {
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Gemini API returned: " + response.getStatusCode());
        }
        
        JSONObject jsonObject = new JSONObject(response.getBody());
        
        if (!jsonObject.has("candidates")) {
            throw new RuntimeException("Invalid Gemini response format");
        }

        String content = jsonObject.getJSONArray("candidates")
                                  .getJSONObject(0)
                                  .getJSONObject("content")
                                  .getJSONArray("parts")
                                  .getJSONObject(0)
                                  .getString("text");

        // Clean JSON from Gemini response
        content = extractJSON(content);
        
        String modelVersion = jsonObject.optString("modelVersion", "gemini-pro");
        
        return processAIResponse(content, request, imageModel, modelVersion, "gemini", ragPerformance);
    }

    private ResponseEntity<?> processAIResponse(String content, HealingModel request, 
                                              Optional<ImageDataModel> imageModel, String modelName, String source, RagPerformanceModel ragPerformance) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonResponse = mapper.readTree(content);
            
            String healedLocator = jsonResponse.path("locator").asText(request.getLocator().replaceAll("\"", "'"));
            String approach = jsonResponse.path("approach").asText("not_match");
            double score = jsonResponse.path("confidence_score").asDouble(0.0);
            
            // Validation
            if (healedLocator.isEmpty()) {
                healedLocator = request.getLocator().replaceAll("\"", "'");
            }

            // Additional check: if approach is "healing" but healed locator equals original, 
            // it should be "already_present"
            if ("healing".equals(approach) && healedLocator.equals(request.getLocator().replaceAll("\"", "'"))) {
                approach = "already_present";
                score = 1.0;
            }
            
            // Save to database
            HealedElement dataHeal = new HealedElement();
            dataHeal.setApproach(approach);
            dataHeal.setHealedLocator(healedLocator);
            dataHeal.setLocator(request.getLocator().replaceAll("\"", "'"));
            dataHeal.setPageSource(request.getPageSource());
            dataHeal.setConfidenceScore(score);
            dataHeal.setModelName(modelName);
            
            if (imageModel.isPresent()) {
                dataHeal.setImageHexId(imageModel.get().getImageHexId());
            }
            
            healRepo.save(dataHeal);
            
            // Update RAG performance with healing results
            ragPerformance.setHealedElementId(dataHeal.getId());
            ragPerformance.setSuccess(true);
            
            // Calculate confidence improvement (if we have previous attempts)
            Optional<HealedElement> previousAttempt = healRepo.findTopByLocatorOrderByConfidenceScoreDesc(request.getLocator().replaceAll("\"", "'"));
            if (previousAttempt.isPresent() && !previousAttempt.get().getId().equals(dataHeal.getId())) {
                double improvement = score - previousAttempt.get().getConfidenceScore();
                ragPerformance.setConfidenceScoreImprovement(improvement);
            }
            
            ragPerformanceRepo.save(ragPerformance);
            
            if (imageModel.isPresent()) {
                imageModel.get().setHealedElementId(healRepo.findByLocator(request.getLocator().replaceAll("\"", "'")).get().getId());
                imageRepo.save(imageModel.get());
            }
            
            // Save training data with RAG-optimized context
            TrainingModel trainingModel = new TrainingModel();
            String ragOptimizedContext = ragService.retrieveRelevantContext(request.getLocator().replaceAll("\"", "'"), request.getPageSource());
            trainingModel.setPropmt(OPTIMIZED_SYSTEM_PROMPT + OUTPUT_FORMAT + EXAMPLES + 
                                  "\n\nFailed locator: " + request.getLocator().replaceAll("\"", "'") + 
                                  "\nRAG-Optimized Context: " + ragOptimizedContext);
            trainingModel.setCompletion(content);
            trainingRepo.save(trainingModel);
            
            Map<String, Object> mapResp = CommonUtility.convertDtoToMap(dataHeal);
            mapResp.remove("page_source");
            mapResp.put("source", source); // Track which API was used
            mapResp.put("rag_stats", Map.of(
                "original_length", ragPerformance.getOriginalPageSourceLength(),
                "optimized_length", ragPerformance.getOptimizedContextLength(),
                "compression_ratio", String.format("%.2f%%", ragPerformance.getCompressionRatio()),
                "processing_time_ms", ragPerformance.getProcessingTimeMs()
            ));
            
            return new ResponseEntity<>(mapResp, HttpStatusCode.valueOf(HttpStatus.OK.value()));
            
        } catch (Exception e) {
            LOGGER.error("Failed to process AI response: " + content, e);
            ragPerformance.setSuccess(false);
            ragPerformance.setErrorMessage("Failed to process AI response: " + e.getMessage());
            ragPerformanceRepo.save(ragPerformance);
            throw new RuntimeException("Invalid AI response format: " + e.getMessage());
        }
    }

    @PostMapping("/is_healed")
    public ResponseEntity<?> isLocatorHealed(@RequestBody HealingModel request){
        Optional<HealedElement> isPresent = healRepo.findByLocatorAndConfidenceScoreGreaterThanEqual(request.getLocator().replaceAll("\"", "'"),.6);
        Optional<HealedElement> isPresentWithFail = healRepo.findTopByLocatorOrderByConfidenceScoreDesc(request.getLocator().replaceAll("\"", "'"));
        Map<String,Object> resMap = new HashMap<>();
        if(isPresent.isPresent()){
            resMap = CommonUtility.convertDtoToMap(isPresent.get());
            resMap.remove("page_source");
            return new ResponseEntity<>(resMap, HttpStatusCode.valueOf(HttpStatus.OK.value()));
        }
        else if(isPresentWithFail.isPresent()){
            resMap = CommonUtility.convertDtoToMap(isPresentWithFail.get());
            resMap.remove("page_source");
            return new ResponseEntity<>(resMap, HttpStatusCode.valueOf(HttpStatus.OK.value()));
        }
        else{
            return new ResponseEntity<>(new ErrorModel().errorResp(400, "Locator not present in Healed DB : "+request.getLocator().replaceAll("\"", "'"))
            , HttpStatusCode.valueOf(HttpStatus.BAD_REQUEST.value()));
        }
    }
    
    @PostMapping("/set-locator-status")
    public ResponseEntity<?> setLocatorStatusOnDB(@RequestBody HealingModel request) {
        Optional<HealedElement> isPresentWithFail = healRepo.findTopByLocatorOrderByConfidenceScoreDesc(request.getLocator().replaceAll("\"", "'"));
        if(isPresentWithFail.isPresent()){
            HealedElement newObj =isPresentWithFail.get();
            newObj.setStatus("Valid");
            newObj.setPageSource(request.getPageSource());
            return new ResponseEntity<>(healRepo.save(newObj), HttpStatusCode.valueOf(HttpStatus.OK.value()));
            
        }
         return new ResponseEntity<>(new ErrorModel().errorResp(400, "Locator not present in Healed DB : "+request.getLocator().replaceAll("\"", "'"))
            , HttpStatusCode.valueOf(HttpStatus.BAD_REQUEST.value()));

    }

    @PostMapping("/image/upload-image")
    public ResponseEntity<?> imageUpload(@RequestParam("file") MultipartFile file){
        ImageDataModel dataModel;
        try{
             dataModel = imageService.saveImage(file);
        }
        catch(Exception e){
            LOGGER.info("Exception Occured while saving image: "+e);
            return new ResponseEntity<>(new ErrorModel().errorResp(400, "Exception Occured : "+e.getMessage()),HttpStatusCode.valueOf(400));
        }
        Map<String,Object> resp = new HashMap<>();
        resp = CommonUtility.convertDtoToMap(dataModel);
        resp.remove("base64");
        return new ResponseEntity<>(resp,HttpStatusCode.valueOf(HttpStatus.CREATED.value()));
    }

    // Download endpoint
    @GetMapping("/image/{id}")
    public ResponseEntity<?> downloadImage(@PathVariable String id){
        try{
            GridFSFile file = imageService.getFileById(id);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        GridFsResource resource = imageService.getFileResource(file);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getMetadata().getString("_contentType")))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                .body(new InputStreamResource(resource.getInputStream()));
        }
        catch (Exception e){
            LOGGER.info("Exception Occured while saving image: "+e);
            return new ResponseEntity<>(new ErrorModel().errorResp(400, "Exception Occured : "+e.getMessage()),HttpStatusCode.valueOf(400));
        }
    }

    // Metadata endpoint
    @GetMapping("/image/meta/{id}")
    public ResponseEntity<?> getFileMeta(@PathVariable String id) {
        GridFSFile file = imageService.getFileById(id);
        Optional<ImageDataModel> imageModel=imageRepo.findByImageHexId(id);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String,Object> resMap= new HashMap<>();
        resMap.put("file_name", file.getFilename());
        resMap.put("length", file.getLength());
        resMap.put("content/type", file.getMetadata().get("_contentType"));
        resMap.put("base64", imageModel.get().getBase64());
        return new ResponseEntity<>(resMap,HttpStatusCode.valueOf(200));
    }

    // RAG Performance and Statistics Endpoints
    
    @GetMapping("/rag/stats")
    public ResponseEntity<?> getRagStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Get recent successful operations
            //PageRequest recent = PageRequest.of(0, 100);
            List<RagPerformanceModel> recentOps = ragPerformanceRepo.findAllSuccessfulOperationsCompressionRatio();
            
            if (!recentOps.isEmpty()) {
                // Calculate average compression ratio
                double avgCompression = recentOps.stream()
                    .filter(op -> op.getCompressionRatio() != null)
                    .mapToDouble(RagPerformanceModel::getCompressionRatio)
                    .average()
                    .orElse(0.0);
                
                // Calculate average processing time
                double avgProcessingTime = recentOps.stream()
                    .filter(op -> op.getProcessingTimeMs() != null)
                    .mapToLong(RagPerformanceModel::getProcessingTimeMs)
                    .average()
                    .orElse(0.0);
                
                // Calculate average context reduction
                double avgOriginalLength = recentOps.stream()
                    .filter(op -> op.getOriginalPageSourceLength() != null)
                    .mapToInt(RagPerformanceModel::getOriginalPageSourceLength)
                    .average()
                    .orElse(0.0);
                
                double avgOptimizedLength = recentOps.stream()
                    .filter(op -> op.getOptimizedContextLength() != null)
                    .mapToInt(RagPerformanceModel::getOptimizedContextLength)
                    .average()
                    .orElse(0.0);
                
                stats.put("total_operations", recentOps.size());
                stats.put("avg_compression_ratio", String.format("%.2f%%", avgCompression));
                stats.put("avg_processing_time_ms", String.format("%.2f", avgProcessingTime));
                stats.put("avg_original_length", String.format("%.0f", avgOriginalLength));
                stats.put("avg_optimized_length", String.format("%.0f", avgOptimizedLength));
                stats.put("avg_token_reduction", String.format("%.0f", avgOriginalLength - avgOptimizedLength));
                
                // Automation type distribution
                Map<String, Long> automationTypeStats = recentOps.stream()
                    .filter(op -> op.getAutomationType() != null)
                    .collect(Collectors.groupingBy(
                        RagPerformanceModel::getAutomationType,
                        Collectors.counting()
                    ));
                stats.put("automation_type_distribution", automationTypeStats);
                
                // Success rate
                long totalOps = ragPerformanceRepo.count();
                long successfulOps = recentOps.size();
                stats.put("success_rate", String.format("%.2f%%", (successfulOps * 100.0) / totalOps));
            } else {
                stats.put("message", "No RAG operations found");
            }
            
            return new ResponseEntity<>(stats, HttpStatus.OK);
            
        } catch (Exception e) {
            LOGGER.error("Error getting RAG statistics: " + e.getMessage(), e);
            return new ResponseEntity<>(
                new ErrorModel().errorResp(500, "Error retrieving RAG statistics: " + e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    @GetMapping("/rag/performance/{locator}")
    public ResponseEntity<?> getRagPerformanceForLocator(@PathVariable String locator) {
        try {
            List<RagPerformanceModel> performances = ragPerformanceRepo.findByLocator(locator);
            
            if (performances.isEmpty()) {
                return new ResponseEntity<>(
                    new ErrorModel().errorResp(404, "No RAG performance data found for locator: " + locator),
                    HttpStatus.NOT_FOUND
                );
            }
            
            // Calculate performance metrics for this specific locator
            Map<String, Object> locatorStats = new HashMap<>();
            locatorStats.put("total_attempts", performances.size());
            
            long successfulAttempts = performances.stream()
                .mapToLong(p -> p.getSuccess() != null && p.getSuccess() ? 1 : 0)
                .sum();
            
            locatorStats.put("successful_attempts", successfulAttempts);
            locatorStats.put("success_rate", String.format("%.2f%%", (successfulAttempts * 100.0) / performances.size()));
            
            // Get best performance
            Optional<RagPerformanceModel> bestPerformance = performances.stream()
                .filter(p -> p.getSuccess() != null && p.getSuccess())
                .min(Comparator.comparingLong(RagPerformanceModel::getProcessingTimeMs));
            
            if (bestPerformance.isPresent()) {
                RagPerformanceModel best = bestPerformance.get();
                locatorStats.put("best_performance", Map.of(
                    "compression_ratio", String.format("%.2f%%", best.getCompressionRatio()),
                    "processing_time_ms", best.getProcessingTimeMs(),
                    "optimized_length", best.getOptimizedContextLength(),
                    "original_length", best.getOriginalPageSourceLength()
                ));
            }
            
            locatorStats.put("recent_performances", performances.stream()
                .limit(5)
                .map(p -> CommonUtility.convertDtoToMap(p))
                .collect(Collectors.toList()));
            
            return new ResponseEntity<>(locatorStats, HttpStatus.OK);
            
        } catch (Exception e) {
            LOGGER.error("Error getting RAG performance for locator: " + e.getMessage(), e);
            return new ResponseEntity<>(
                new ErrorModel().errorResp(500, "Error retrieving RAG performance: " + e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    @GetMapping("/rag/health")
    public ResponseEntity<?> ragHealthCheck() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("rag_enabled", ragService != null && contextAnalyzerService != null);
            health.put("configuration_loaded", ragConfig != null);
            health.put("timestamp", System.currentTimeMillis());
            health.put("status", "RAG services are operational");
            
            return new ResponseEntity<>(health, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.error("RAG health check failed: " + e.getMessage(), e);
            return new ResponseEntity<>(
                Map.of("status", "RAG services unavailable", "error", e.getMessage()),
                HttpStatus.SERVICE_UNAVAILABLE
            );
        }
    }
    
    @PostMapping("/rag/test")
    public ResponseEntity<?> testRagExtraction(@RequestBody Map<String, String> request) {
        try {
            String locator = request.get("locator");
            String pageSource = request.get("page_source");
            Boolean enableDebug = Boolean.parseBoolean(request.getOrDefault("debug", "false"));
            
            if (locator == null || pageSource == null) {
                return new ResponseEntity<>(
                    new ErrorModel().errorResp(400, "Both 'locator' and 'page_source' are required"),
                    HttpStatus.BAD_REQUEST
                );
            }
            
            // Enable debug mode if requested
            if (enableDebug) {
                // Temporarily enable detailed logging for this test
                LOGGER.info("RAG Debug Test - Locator: " + locator);
                LOGGER.info("RAG Debug Test - Page Source Length: " + pageSource.length());
                LOGGER.info("RAG Debug Test - Page Source Preview: " + 
                           pageSource.substring(0, Math.min(200, pageSource.length())) + "...");
            }
            
            // Test RAG extraction
            boolean isValid = ragService.validateRagOutput(locator, pageSource);
            String extractedContext = ragService.retrieveRelevantContext(locator, pageSource);
            RagService.ContextStats stats = ragService.getContextStats(locator, pageSource);
            
            Map<String, Object> result = new HashMap<>();
            result.put("validation_passed", isValid);
            result.put("context_length", extractedContext.length());
            result.put("context_empty", extractedContext.trim().isEmpty());
            result.put("original_length", stats.originalLength);
            result.put("compression_ratio", String.format("%.2f%%", 100.0 - stats.compressionRatio));
            result.put("extracted_context", extractedContext);
            result.put("stats", stats);
            
            // Add debug information if enabled
            if (enableDebug) {
                result.put("locator_analysis", analyzeLocatorForDebug(locator));
                result.put("page_source_preview", pageSource.substring(0, Math.min(500, pageSource.length())));
                
                // Show the 5 chunks analysis
                result.put("chunk_analysis", analyzeChunksForDebug(locator, pageSource));
                
                // Check if the expected element is in the page source
                boolean elementFound = pageSource.toLowerCase().contains("search_image") || 
                                     pageSource.toLowerCase().contains(locator.toLowerCase());
                result.put("target_element_found_in_source", elementFound);
            }
            
            if (!isValid || extractedContext.trim().isEmpty()) {
                result.put("warning", "RAG returned empty context - this indicates a configuration or logic issue");
                LOGGER.warn("RAG test failed for locator: " + locator);
            }
            
            return new ResponseEntity<>(result, HttpStatus.OK);
            
        } catch (Exception e) {
            LOGGER.error("Error testing RAG extraction: " + e.getMessage(), e);
            return new ResponseEntity<>(
                new ErrorModel().errorResp(500, "Error testing RAG: " + e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PostMapping("/rag/analyze-context")
    public ResponseEntity<?> analyzePageContext(@RequestBody Map<String, String> request) {
        try {
            String pageSource = request.get("page_source");
            String locator = request.get("locator");
            
            if (pageSource == null) {
                return new ResponseEntity<>(
                    new ErrorModel().errorResp(400, "page_source is required"),
                    HttpStatus.BAD_REQUEST
                );
            }
            
            // Analyze page structure
            ContextAnalyzerService.PageAnalysis analysis = contextAnalyzerService.analyzePage(pageSource);
            
            Map<String, Object> result = new HashMap<>();
            result.put("page_analysis", Map.of(
                "automation_type", analysis.automationType.toString(),
                "form_elements_count", analysis.formElements.size(),
                "interactive_elements_count", analysis.interactiveElements.size(),
                "elements_with_ids_count", analysis.elementsWithIds.size(),
                "elements_with_classes_count", analysis.elementsWithClasses.size(),
                "text_content_count", analysis.textContent.size()
            ));
            
            // If locator is provided, get RAG context
            if (locator != null && !locator.trim().isEmpty()) {
                RagService.ContextStats contextStats = ragService.getContextStats(locator, pageSource);
                result.put("rag_optimization", Map.of(
                    "original_length", contextStats.originalLength,
                    "optimized_length", contextStats.optimizedLength,
                    "compression_achieved", String.format("%.2f%%", 100.0 - contextStats.compressionRatio),
                    "relevant_context", ragService.retrieveRelevantContext(locator, pageSource)
                ));
                
                // Find similar elements
                List<String> similarElements = contextAnalyzerService.findSimilarElements(locator, pageSource);
                result.put("similar_elements", similarElements.stream().limit(5).collect(Collectors.toList()));
            }
            
            return new ResponseEntity<>(result, HttpStatus.OK);
            
        } catch (Exception e) {
            LOGGER.error("Error analyzing context: " + e.getMessage(), e);
            return new ResponseEntity<>(
                new ErrorModel().errorResp(500, "Error analyzing context: " + e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PostMapping("/is_already_present")
    public ResponseEntity<?> isLocatorPresent(@RequestBody HealingModel request){
        Boolean isPresent = LocatorMatcher.isLocatorPresent(request.getLocator(), request.getPageSource(), ContextAnalyzerService.detectAutomationType(request.getPageSource()));
        return new ResponseEntity<>(new ErrorModel().errorResp(200, "Locator not present in Page Source : "+isPresent)
            , HttpStatusCode.valueOf(HttpStatus.OK.value()));
    }

    @PostMapping("/heal_locally")
    public ResponseEntity<?> healLocally(@RequestBody HealingModel request){
        HashMap<String,Object> map = LocatorMatcher.healLocator(request.getLocator(), request.getPageSource());
        return new ResponseEntity<>(map, HttpStatusCode.valueOf(HttpStatus.OK.value()));
    }

    @Data
    public class Message {
        public String role;
        public String content;
    }
    @Data
    public class ResponseFormat{
        public String type;
    }

    @Data
    public class AiRequestBody extends BaseModel {
        public ArrayList<Message> messages = new ArrayList<>();
        public String model;
        public double temperature;
        public int max_tokens;
        public  ResponseFormat response_format = new ResponseFormat();
        public AiRequestBody(){
            response_format.setType("json_object");
        }
        public void setMessage(String role, String content) {
            Message obj = new Message();
            obj.setRole(role);
            obj.setContent(content);
            this.messages.add(obj);
        }
        
    }

    @Data
    public class GeminiRequestBody extends BaseModel  {
        private List<Content> contents = new ArrayList<>();
        private SystemInstruction systemInstruction = new SystemInstruction();
        private GenerationConfig generationConfig = new GenerationConfig();

        public GeminiRequestBody(String systemPrompt, String userPrompt) {
            // System instruction
            Part systemPart = new Part();
            systemPart.setText(systemPrompt);
            systemInstruction.getParts().add(systemPart);

            // User content
            Part userPart = new Part();
            userPart.setText(userPrompt);
            Content userContent = new Content();
            userContent.getParts().add(userPart);
            contents.add(userContent);
        }

        @Data
        public static class Content {
            private String role = "user";
            private List<Part> parts = new ArrayList<>();
        }

        @Data
        public static class GenerationConfig {
            private double temperature = 0.05; // Lower temperature for more consistent results
            private int maxOutputTokens = 5000;
            private String responseMimeType = "application/json";
        }

        @Data
        public static class Part {
            private String text;
        }

        @Data
        public static class SystemInstruction {
            private List<Part> parts = new ArrayList<>();
        }
    }

    // Utility method to truncate page source if too large
    private String truncatePageSource(String pageSource, int maxLength) {
        /* 
        if (pageSource == null) return "";
        
        if (pageSource.length() <= maxLength) {
            return pageSource;
        }

        // Try to keep the structure by finding good break points
        String truncated = pageSource.substring(0, maxLength);
        int lastTagClose = truncated.lastIndexOf(">");
        int lastTagOpen = truncated.lastIndexOf("<");
        
        // If we have a complete tag structure, use that
        if (lastTagClose > lastTagOpen && lastTagClose > maxLength - 100) {
            return pageSource.substring(0, lastTagClose + 1) + "\n<!-- ... truncated ... -->";
        }
        
        return truncated + "\n<!-- ... truncated ... -->";*/
        return pageSource;
    }

    // Enhanced text validation methods
    private boolean validateTextInPageSource(String locator, String pageSource) {
        try {
            // Extract text values from locator using regex
            Pattern textPattern = Pattern.compile("contains\\([^,]+,\\s*['\"]([^'\"]+)['\"]\\)");
            Matcher matcher = textPattern.matcher(locator);
            
            while (matcher.find()) {
                String searchText = matcher.group(1);
                LOGGER.info("Validating text in locator: '" + searchText + "'");
                
                // Check for exact match (case-sensitive)
                if (pageSource.contains("text=\"" + searchText + "\"") || 
                    pageSource.contains("text='" + searchText + "'")) {
                    LOGGER.info("Exact text match found: " + searchText);
                    return true;
                }
                
                // Check for case-insensitive match
                String lowerSearchText = searchText.toLowerCase();
                String lowerPageSource = pageSource.toLowerCase();
                
                if (lowerPageSource.contains("text=\"" + lowerSearchText + "\"") || 
                    lowerPageSource.contains("text='" + lowerSearchText + "'")) {
                    LOGGER.info("Case-insensitive text match found: " + searchText);
                    return true;
                }
                
                // Check for partial match with different spacing
                String normalizedSearchText = searchText.replaceAll("\\s+", "\\s+");
                Pattern flexibleTextPattern = Pattern.compile("text=['\"][^'\"]*" + 
                    Pattern.quote(searchText).replace("\\s+", "\\s*") + "[^'\"]*['\"]", 
                    Pattern.CASE_INSENSITIVE);
                
                if (flexibleTextPattern.matcher(pageSource).find()) {
                    LOGGER.info("Flexible text match found: " + searchText);
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            LOGGER.warn("Error validating text in page source: " + e.getMessage());
            return false;
        }
    }

    // Enhanced prompt building with RAG-optimized context
    private String buildUserPrompt(String locator, String pageSource, List<HealedElement> examples) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("TASK: Analyze and heal the failed locator\n\n");
        prompt.append("Failed Locator: ").append(locator).append("\n");
        
        // Detect and highlight OR conditions
        boolean hasOrConditions = locator.contains(" or ") || locator.contains("|");
        if (hasOrConditions) {
            prompt.append("\n🔍 IMPORTANT: This locator contains OR conditions!\n");
            prompt.append("Remember: If ANY OR condition matches the page source, return the EXACT original locator with approach='already_present'\n");
            prompt.append("Do NOT simplify or remove working OR conditions!\n\n");
        }

        // Detect and validate text-based locators
        boolean hasTextSearch = locator.contains("contains(@text,") || locator.contains("contains(translate(@text");
        if (hasTextSearch) {
            prompt.append("🔍 TEXT-BASED LOCATOR DETECTED!\n");
            prompt.append("CRITICAL: Verify the exact text exists in page source before making any changes.\n");
            prompt.append("Do NOT modify text content unless the text is completely absent from page source.\n");
            prompt.append("Preserve spacing, capitalization, and formatting exactly as provided.\n\n");
            
            // Pre-validate text existence
            if (validateTextInPageSource(locator, pageSource)) {
                prompt.append("✅ TEXT VALIDATION: The text in this locator appears to exist in the page source.\n");
                prompt.append("This locator should likely be marked as 'already_present' - do not modify the text!\n\n");
            } else {
                prompt.append("⚠️ TEXT VALIDATION: The exact text may not be found in page source.\n");
                prompt.append("Carefully check for similar text before suggesting modifications.\n\n");
            }
        }
        
        // Add relevant examples if available
        if (examples != null && !examples.isEmpty()) {
            prompt.append("SIMILAR SUCCESSFUL CASES:\n");
            for (int i = 0; i < Math.min(2, examples.size()); i++) {
                HealedElement ex = examples.get(i);
                prompt.append("- Original: ").append(ex.getLocator())
                    .append(" → Healed: ").append(ex.getHealedLocator())
                    .append(" (confidence: ").append(ex.getConfidenceScore()).append(")\n");
            }
            prompt.append("\n");
        }
        
        // Use RAG-optimized context instead of full page source
        String optimizedContext = ragService.retrieveRelevantContext(locator, pageSource);
        LOGGER.info("Optimised Page Source"+optimizedContext);
        prompt.append("Relevant Page Context (RAG-optimized):\n").append(optimizedContext);
        return prompt.toString();
    }

    Map<String, String> setHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + JsonConfig.config.getMockObject().get("GROQ_API_KEY"));
        return headers;
    }

    Map<String, String> setGeminiHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return headers;
    }


    private String extractJSON(String content) {
        // Remove markdown formatting
        Pattern jsonPattern = Pattern.compile("(?s)```json\\s*(.*?)\\s*```");
        Matcher matcher = jsonPattern.matcher(content);
        
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        // Try to find JSON object directly
        Pattern objectPattern = Pattern.compile("(?s)\\{.*\\}");
        Matcher objectMatcher = objectPattern.matcher(content);
        
        if (objectMatcher.find()) {
            return objectMatcher.group().trim();
        }
        
        return content;
    }

    private Map<String, Object> analyzeLocatorForDebug(String locator) {
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("original_locator", locator);
        analysis.put("is_xpath_format", locator.startsWith("//") || locator.startsWith("/"));
        analysis.put("is_resource_id_format", locator.matches("^[a-zA-Z0-9._]+:[a-zA-Z0-9_]+/[a-zA-Z0-9_]+$"));
        analysis.put("contains_resource_id", locator.contains(":id/"));
        analysis.put("contains_package_name", locator.contains("."));
        
        if (locator.contains(":id/")) {
            String[] parts = locator.split(":id/");
            if (parts.length == 2) {
                analysis.put("package_name", parts[0]);
                analysis.put("element_name", parts[1]);
            }
        }
        
        return analysis;
    }
    
    private Map<String, Object> analyzeChunksForDebug(String locator, String pageSource) {
        Map<String, Object> chunkAnalysis = new HashMap<>();
        
        try {
            // Use the same logic as RAG service to divide into 5 chunks
            int totalLength = pageSource.length();
            int chunkSize = totalLength / 5;
            
            List<Map<String, Object>> chunks = new ArrayList<>();
            
            for (int i = 0; i < 5; i++) {
                int start = i * chunkSize;
                int end = (i == 4) ? totalLength : Math.min(start + chunkSize, totalLength);
                
                if (start < totalLength) {
                    String chunk = pageSource.substring(start, end);
                    
                    Map<String, Object> chunkInfo = new HashMap<>();
                    chunkInfo.put("chunk_number", i + 1);
                    chunkInfo.put("start_position", start);
                    chunkInfo.put("end_position", end);
                    chunkInfo.put("length", chunk.length());
                    chunkInfo.put("preview", chunk.substring(0, Math.min(200, chunk.length())) + 
                                           (chunk.length() > 200 ? "..." : ""));
                    chunkInfo.put("contains_locator", chunk.toLowerCase().contains(locator.toLowerCase()));
                    
                    // Check for resource ID parts
                    boolean containsResourceId = false;
                    if (locator.contains(":id/")) {
                        String[] parts = locator.split(":id/");
                        if (parts.length == 2) {
                            containsResourceId = chunk.toLowerCase().contains(parts[1].toLowerCase()) ||
                                               chunk.toLowerCase().contains(parts[0].toLowerCase());
                        }
                    }
                    chunkInfo.put("contains_resource_id_parts", containsResourceId);
                    
                    chunks.add(chunkInfo);
                }
            }
            
            chunkAnalysis.put("total_chunks", chunks.size());
            chunkAnalysis.put("chunk_size_avg", chunkSize);
            chunkAnalysis.put("chunks", chunks);
            
        } catch (Exception e) {
            chunkAnalysis.put("error", "Failed to analyze chunks: " + e.getMessage());
        }
        
        return chunkAnalysis;
    }

    private HealedElement locatorPresentInPageSource(String locator, String updatedLocator){
        HealedElement healedElement = new HealedElement();
        healedElement.setLocator(locator);
        healedElement.setApproach("already_present");
        healedElement.setConfidenceScore(1.0);
        healedElement.setHealedLocator(updatedLocator);
        return healedElement;
    }
}
