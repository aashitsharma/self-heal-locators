package onemg.analytics.dump.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import onemg.analytics.dump.JsonConfig;
import onemg.analytics.dump.model.ErrorModel;
import onemg.analytics.dump.model.HealedElement;
import onemg.analytics.dump.model.HealingModel;
import onemg.analytics.dump.model.ImageDataModel;
import onemg.analytics.dump.model.TrainingModel;
import onemg.analytics.dump.repository.HealedElemetRepository;
import onemg.analytics.dump.repository.ImageDataRepository;
import onemg.analytics.dump.repository.TrainingModelRepository;
import onemg.analytics.dump.utils.CommonUtility;
import onemg.analytics.dump.utils.ImageService;

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

private final ImageService imageService;

 @Autowired
public OpenAI(ImageService imageService) {
    this.imageService = imageService;
}

    private static final Logger LOGGER = Logger.getLogger(OpenAI.class);
    static String SystemPrompt="";
    static String examplePrompt="";
    static String outro="";
    static String gemeniOutput="";
    static String gorqOutput="";
    static{
         SystemPrompt = "##You are an AI Automation Locator Healing Agent with deep expertise in:\n\n"
        + "* Selenium\n"
        + "* Appium\n"
        + "* UIAutomator2\n"
        + "* XCUITest\n"
        + "* Other modern UI automation frameworks\n\n"
        + "##Your responsibility is to analyze a failed locator (provided by the user) against a given page source (HTML or XML).\n\n"
        + "##You must intelligently identify whether the locator is:\n\n"
        + "* Already correct and present in the given page source.\n"
        + "* Incorrect but can be healed into the most probable correct locator.\n"
        + "* Completely unmatched with no confident correction.\n\n"
        + "## Instructions for Processing\n\n"
        + "1. Parse the given page source (HTML or XML).\n\n"
        + "2. Consider all relevant attributes for maximum accuracy in locator matching:\n\n"
        + " * id, class, name, text, content-desc, xpath, aria-label, accessibilityId, etc.\n"
        + " * Class tag matches the XML element exactly\n"
        + " * resource-id matches precisely\n"
        + " * Visibility & displayed attributes.\n"
        + " * Hierarchical relationships (parent → child → sibling).\n"
        + " * Element is uniquely identified within the hierarchy\n"
        + " * Compare the failed locator with available elements in the page source.\n"
        + " * Compute a match score (0-100%) using similarity of attributes and structural position.\n"
        + "    - Normalize this score as a decimal between 0.0 and 1.0 (example: 80% → 0.80).\n\n"
        + "3. Respect the or(|) operator present in xpath. If any of the condition present inside the locator which are seperated by or(|) is correct and present in the page source(XML/HTML) then return the same.\n\n"; 
    }
    static{
        gorqOutput= "## Output Rules\n\n"
        + " * Always returns a Xpath locator, this is a strict and uncompromisable rule"
        + " * If the failed locator is already correct and present in the page source OR if any of the locator present in or(|) condition and is correct → return:\n\n"
        + "{\n"
        + "  \"locator\": \"<failed locator passed by user>\",\n"
        + "  \"approach\": \"already_present\",\n"
        + "  \"confidence_score\": 1.0\n"
        + "}\n\n"
        + " * If the best possible healed locator has 60-100% match score → return:\n\n"
        + "{\n"
        + "  \"locator\": \"<correct healed locator>\",\n"
        + "  \"approach\": \"healing\",\n"
        + "  \"confidence_score\": <decimal score between 0.60-1.0>\n"
        + "}\n\n"
        + " * If the match score is below 60% → return:\n\n"
        + "{\n"
        + "  \"locator\": \"<failed locator passed by user>\",\n"
        + "  \"approach\": \"not_match\",\n"
        + "  \"confidence_score\": <decimal score below 0.60>\n"
        + "}\n\n";
    }
    static{
        gemeniOutput= "## Output Rules\n\n"
        + " * Always returns a Xpath locator, this is a strict and uncompromisable rule\n\n"
        + " * Need to provide a brief reasoning on the basis of which the outcome is decided and DON NOT use \" or any special character which could break the String data santity in between, remove if there are any."
        + "    - Reasoning should be always JSON acceptable string.This is a strict and uncompromisable rule \n\n"
        + " * Output should be always a Valid JSON object, this is a strict and uncompromisable rule\n\n"
        + " * If the failed locator is already correct and present in the page source OR if any of the locator present in or(|) condition and is correct→ return:\n\n"
        + "{\n"
        + "  \"locator\": \"<failed locator passed by user>\",\n"
        + "  \"approach\": \"already_present\",\n"
        + "  \"confidence_score\": 1.0,\n"
        +"   \"reasoning\": \"<Main Reason against which the final outcome is decided>\"\n"
        + "}\n\n"
        + " * If the best possible healed locator has 60-100% match score → return:\n\n"
        + "{\n"
        + "  \"locator\": \"<correct healed locator>\",\n"
        + "  \"approach\": \"healing\",\n"
        + "  \"confidence_score\": <decimal score between 0.60-1.0>,\n"
        +"   \"reasoning\": \"<Main Reason against which the final outcome is decided>\"\n"
        + "}\n\n"
        + " * If the match score is below 60% → return:\n\n"
        + "{\n"
        + "  \"locator\": \"<failed locator passed by user>\",\n"
        + "  \"approach\": \"not_match\",\n"
        + "  \"confidence_score\": <decimal score below 0.60>,\n"
        +"   \"reasoning\": \"<Main Reason against which the final outcome is decided>\"\n"
        + "}\n\n"
        +" Core rule - Respond Strictly ONLY in valid JSON with the following schema:\n" + 
                        "{\n" + 
                        "  \"locator\": \"...\",\n" + 
                        "  \"approach\": \"...\",\n" + 
                        "  \"confidence_score\": 0.0,\n" + 
                        "  \"reasoning\": \"...\"\n" + 
                        "}\n\n";
    }
    static{
        examplePrompt =  "##Example Input:\n"
        + " Failed locator: //button[@id='loginBtn']\n"
        + " Source code: <button type=\\\"submit\\\" id=\\\"loginButton\\\" class=\\\"btn\\\">Login<button>\n\n"
        + "##Example Output:\n"
        + "{\n"
        + "  \"locator\":\"//button[@id='loginButton']\",\n"
        + "  \"approach\": \"healing\",\n"
        + "  \"confidence_score\": 1.0\n"
        + "}\n\n";
    }
    static{
        outro="##Key things which always needs to keep in memory\n\n"
        + "1. Always prefer accurate locators (minimal, stable, and resilient).\n\n"
        + "2. Respect framework-specific strategies (Selenium By.xpath, Appium accessibilityId, UIAutomator2 resource-id, XCUITest name/label, etc.).\n\n"
        + "3. Output must be only the JSON object.\n\n"
        + "4. Do not include 'amp;' in healed locators in case of '&' is present, and same logic for other special characters also(avoid encoding special characters).\n\n"
        + "5. Use Reliable and accurate locators while building the healed locator which should work ony differenct devices.\n\n"
        + " - Use attributes like:\n"
        + "  * resource-id\n"
        + "  * class\n"
        + "  * Hierarchy of elements (Parent/Child/Sibling)\n\n"
        + " - Do not use attributes like:\n"
        + "  * bounds\n"
        + "  * cordinates\n";
    }

    @Data
    public class Message{
        public String role;
        public String content;
    }

    @Data
    public class AiRequestBody{
        public ArrayList<Message> messages = new ArrayList<>();
        public String model;
        public int temperature;
        public String reasoning_effort;

        public void setMessage(String role, String content){
            Message obj = new Message();
            obj.setRole(role);
            obj.setContent(content);
            this.messages.add(obj);
        }
        
    }
    
    Map<String,String> setHeaders(){
        Map<String,String> headers = new HashMap<>();
        headers.put("Content-Type","application/json");
        headers.put("Authorization","Bearer "+JsonConfig.config.getMockObject().get("GROQ_API_KEY"));
        return headers;
    }

    Map<String,String> setGemeniHeaders(){
        Map<String,String> headers = new HashMap<>();
        headers.put("Content-Type","application/json");
        //headers.put("Authorization","Bearer "+JsonConfig.config.getMockObject().get("GEMENI_API_KEY"));
        return headers;
    }
    
    @Data
    public class GemeniRequestBody {

        private List<Content> contents = new ArrayList<>();
        private SystemInstruction systemInstruction = new SystemInstruction();
        private GenerationConfig generationConfig = new GenerationConfig();

        public GemeniRequestBody(String systemPrompt, String userPrompt) {
            // Create system part
            Part system = new Part();
            system.setText(systemPrompt);
            systemInstruction.getParts().add(system);

            // Create user part
            Part user = new Part();
            user.setText(userPrompt);
            Content userContent = new Content();
            userContent.getParts().add(user);

            // Add to contents list
            contents.add(userContent);
        }

        @Data
        public static class Content {
            private String role = "user";
            private List<Part> parts = new ArrayList<>();
        }

        @Data
        public static class GenerationConfig {
            private int temperature = 2;
            private int topP = 1;
            private ThinkingConfig thinkingConfig = new ThinkingConfig();
            private String responseMimeType= "application/json";
        }

        @Data
        public static class Part {
            private String text;
        }

        @Data
        public static class SystemInstruction {
            //private String role = "systemInstruction";
            private List<Part> parts = new ArrayList<>();
        }

        @Data
        public static class ThinkingConfig {
            private int thinkingBudget = 6000;
        }
    }

    @PostMapping("/get-healed-locator")
    public ResponseEntity<?> getHealedLocator(@RequestBody HealingModel request){
        if(null!=request.getLocator() && null!= request.getPageSource()){
            String baseUrl = JsonConfig.config.getMockObject().get("GROQ_API_URL").toString();
        PageRequest topFive = PageRequest.of(0, 5); // first page, size 5
        List<HealedElement> listOfHealElements = healRepo.findByConfidenceScoreGreaterThanEqualOrderByConfidenceScoreDesc(0.60,topFive);
        
        Optional<ImageDataModel> imageModel=imageRepo.findByImageHexId(request.getImageDataId());

        HttpHeaders header = new HttpHeaders();
        header.setAll(setHeaders());
        AiRequestBody requestBody = new AiRequestBody();
        requestBody.setModel("openai/gpt-oss-120b");
        requestBody.setTemperature(2);
        requestBody.setReasoning_effort("high");
        
        requestBody.setMessage("system", SystemPrompt+gorqOutput+examplePrompt+outro);
        String UserPrompt = "Failed locator : "+request.getLocator()+"\n"+"Source code : "+request.getPageSource();
        requestBody.setMessage("user", UserPrompt);
        //requestBody.setMessage("assistant", listOfHealElements.toString());

        HttpEntity<AiRequestBody> entity = new HttpEntity<>(requestBody, header);

        try{
            ResponseEntity<String> response =
                restTemplate.exchange(baseUrl, HttpMethod.POST, entity, String.class);
            JSONObject jsonObject = new JSONObject(response.getBody());
            if(response.getStatusCode()==HttpStatusCode.valueOf(200)){
                String healedLocator = "",approach ="",reasoning="";
            Double score = 0.0;
            JSONObject healedJson = null;
            if (jsonObject.has("choices")) {
                String choice = jsonObject
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
                healedJson = new JSONObject(choice);
            
                healedLocator = healedJson.optString("locator");
                approach = healedJson.optString("approach");
                score = healedJson.getDouble("confidence_score");

                reasoning= jsonObject
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("reasoning");
                LOGGER.info("JSON response from Open AI : "+healedJson);
            }
            HealedElement dataHeal = new HealedElement();
            dataHeal.setApproach(approach);
            dataHeal.setHealedLocator(healedLocator);
            dataHeal.setLocator(request.getLocator());
            dataHeal.setReasoning(reasoning);
            dataHeal.setPageSource(request.getPageSource());
            dataHeal.setConfidenceScore(score);
            dataHeal.setModelName("openai/gpt-oss-120b");
            if(imageModel.isPresent())
                dataHeal.setImageHexId(imageModel.get().getImageHexId());
            healRepo.save(dataHeal);
            if(imageModel.isPresent()){
                imageModel.get().setHealedElementId(healRepo.findByLocator(request.getLocator()).get().getId());
                imageRepo.save(imageModel.get());
            }
            Map<String,Object> mapResp = CommonUtility.convertDtoToMap(dataHeal);
            //mapResp.put("img_base64", imageModel.get().getBase64());
            mapResp.remove("page_source");
            TrainingModel trainingModel = new TrainingModel();
            trainingModel.setPropmt(SystemPrompt+gorqOutput+examplePrompt+outro+"\n\n"+UserPrompt);
            trainingModel.setCompletion(healedJson.toString());
            trainingRepo.save(trainingModel);
            return new ResponseEntity<>(mapResp, HttpStatusCode.valueOf(HttpStatus.OK.value()));
        }
        }
        catch(Exception e){
            
            ResponseEntity<?> responseGemini= getHealedLocatorGemeni(request);
            if(!responseGemini.getStatusCode().is2xxSuccessful()){
                
                Map<String,Object> RespMap= new HashMap<>();
                RespMap.put("healed_locator", request.getLocator());
                RespMap.put("message", "AI Response is : "+e.getMessage());
                return new ResponseEntity<>(RespMap, HttpStatusCode.valueOf(HttpStatus.OK.value()));
             
            }
            
            return responseGemini;
            
        }
        return new ResponseEntity<>(new ErrorModel().errorResp(400, "Both AI Agent got Failed")
            , HttpStatusCode.valueOf(HttpStatus.BAD_REQUEST.value()));
        }
        else{
            return new ResponseEntity<>(new ErrorModel().errorResp(400, "locator and page_source are mandotory feilds")
            , HttpStatusCode.valueOf(HttpStatus.BAD_REQUEST.value()));
        }
    }

    @PostMapping("/get-healed-locator/gemeni")
    public ResponseEntity<?> getHealedLocatorGemeni(@RequestBody HealingModel request){
        if(null!=request.getLocator() && null!= request.getPageSource()){
            //GEMENI_API_KEY
            //GEMENI_API_URL
            Optional<ImageDataModel> imageModel=imageRepo.findByImageHexId(request.getImageDataId());
            String baseUrl = JsonConfig.config.getMockObject().get("GEMENI_API_URL").toString();
            HttpHeaders header = new HttpHeaders();
            header.setAll(setGemeniHeaders());

            String UserPrompt = "Failed locator : "+request.getLocator()+"\n"+"Source code : "+request.getPageSource();
            GemeniRequestBody reqBody = new GemeniRequestBody(SystemPrompt+gorqOutput+examplePrompt+outro, UserPrompt);

            HttpEntity<GemeniRequestBody> entity = new HttpEntity<>(reqBody, header);
            ResponseEntity<String> response =
                restTemplate.exchange(baseUrl+"?key="+JsonConfig.config.getMockObject().get("GEMENI_API_KEY"), HttpMethod.POST, entity, String.class);
            JSONObject jsonObject = new JSONObject(response.getBody());
            LOGGER.info("Gemeni Response Body : "+jsonObject.toString());
            if(response.getStatusCode()==HttpStatusCode.valueOf(200)){
                String healedLocator = "",approach ="",reasoning="";
                Double score = 0.0;
                if (jsonObject.has("candidates")) {
                    String choice = jsonObject
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");
                    
                    
                    // Extract JSON block inside ```json ... ```
                    Pattern pattern = Pattern.compile("(?s)```json(.*?)```");
                    Matcher matcher = pattern.matcher(choice);
                    if (matcher.find()) {
                        choice = matcher.group(1).trim();
                    }

                    // Clean up leading/trailing junk
                    choice = choice.replaceAll("(?m)^\\s*\\{", "{");  // ensure starts with {
                    choice = choice.replaceAll("}\\s*$", "}");        // ensure ends with }

                    // Try parsing with Jackson (more tolerant than org.json)
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode healedJson;
                    try {
                        healedJson = mapper.readTree(choice);
                    } catch (Exception e) {
                        LOGGER.error("Invalid JSON from Gemini, raw text: " + choice, e);
                        throw new RuntimeException("Failed to parse Gemini response", e);
                    }
                    // Extract fields safely
                     healedLocator = healedJson.path("locator").asText("");
                     approach = healedJson.path("approach").asText("");
                     score = healedJson.path("confidence_score").asDouble(0.0);
                     //reasoning = healedJson.path("reasoning").asText("");
                    LOGGER.info("JSON response from Gemeni : "+healedJson);
                    HealedElement dataHeal = new HealedElement();
                    dataHeal.setApproach(approach);
                    dataHeal.setHealedLocator(healedLocator);
                    dataHeal.setLocator(request.getLocator());
                    //dataHeal.setReasoning(reasoning);
                    dataHeal.setPageSource(request.getPageSource());
                    dataHeal.setConfidenceScore(score);
                    dataHeal.setModelName(jsonObject.getString("modelVersion"));
                    if(imageModel.isPresent())
                        dataHeal.setImageHexId(imageModel.get().getImageHexId());
                    healRepo.save(dataHeal);
                    if(imageModel.isPresent()){
                        imageModel.get().setHealedElementId(healRepo.findByLocator(request.getLocator()).get().getId());
                        imageRepo.save(imageModel.get());
                    }
                    Map<String,Object> mapResp = CommonUtility.convertDtoToMap(dataHeal);
                    mapResp.remove("page_source");
                    //mapResp.put("img_base64", imageModel.get().getBase64());
                    TrainingModel trainingModel = new TrainingModel();
                    trainingModel.setPropmt(SystemPrompt+gemeniOutput+examplePrompt+outro+"\n\n"+UserPrompt);
                    trainingModel.setCompletion(healedJson.toPrettyString());
                    trainingRepo.save(trainingModel);
                    return new ResponseEntity<>(mapResp, HttpStatusCode.valueOf(HttpStatus.OK.value()));
                }
                
            }
            return new ResponseEntity<>(response.getBody(), HttpStatusCode.valueOf(HttpStatus.BAD_REQUEST.value()));
        
        }

        else{
            return new ResponseEntity<>(new ErrorModel().errorResp(400, "locator and page_source are mandotory feilds")
            , HttpStatusCode.valueOf(HttpStatus.BAD_REQUEST.value()));
        }
    }

    @PostMapping("/is_healed")
    public ResponseEntity<?> isLocatorHealed(@RequestBody HealingModel request){
        Optional<HealedElement> isPresent = healRepo.findByLocatorAndConfidenceScoreGreaterThanEqual(request.getLocator(),.6);
        Optional<HealedElement> isPresentWithFail = healRepo.findTopByLocatorOrderByConfidenceScoreDesc(request.getLocator());
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
            return new ResponseEntity<>(new ErrorModel().errorResp(400, "Locator not present in Healed DB : "+request.getLocator())
            , HttpStatusCode.valueOf(HttpStatus.BAD_REQUEST.value()));
        }
    }
    
    @PostMapping("/set-locator-status")
    public ResponseEntity<?> setLocatorStatusOnDB(@RequestBody HealingModel request) {
        Optional<HealedElement> isPresentWithFail = healRepo.findTopByLocatorOrderByConfidenceScoreDesc(request.getLocator());
        if(isPresentWithFail.isPresent()){
            HealedElement newObj =isPresentWithFail.get();
            newObj.setStatus("Valid");
            newObj.setPageSource(request.getPageSource());
            return new ResponseEntity<>(healRepo.save(newObj), HttpStatusCode.valueOf(HttpStatus.OK.value()));
            
        }
         return new ResponseEntity<>(new ErrorModel().errorResp(400, "Locator not present in Healed DB : "+request.getLocator())
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

}
