package com.csye6225.assignment1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
//import at.favre.lib.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.*;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.env.Environment;
import javax.persistence.Convert;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller

@RequestMapping(path="/")
public class MainController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private AttachRepository attachmentRepository;

    @Autowired
    private AmazonClient amazonClient;

    @Autowired
    private Environment env;

    @Value("${profile.name}")
    private String profileName;

    public static final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);


    public static final Pattern VALID_PWD_REGEX =
             Pattern.compile("^(?=.*\\d)(?=.*[a-z])(?=.*[@#$%])(?=.*[A-Z]).{8,30}$");

    @PostMapping(path = "/user/register")
    public @ResponseBody
    JEntity addNewUser(@RequestBody User user, HttpServletResponse response) {
        JEntity jEntity = new JEntity();

        if (validateEmail(user.getEmail()) == false) {
            jEntity.setMsg("Please enter a valid email id");

            jEntity.setStatuscode(HttpStatus.FORBIDDEN);
            jEntity.setCode(HttpStatus.FORBIDDEN.value());
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setHeader("status", HttpStatus.FORBIDDEN.toString());
            return jEntity;
        }

         if (validatePwd(user.getpwd())==false){
             jEntity.setMsg("Password should atleast have 1 Lower case, 1 upper case, 1 digit and 1 special character ");

             jEntity.setStatuscode(HttpStatus.EXPECTATION_FAILED);
             jEntity.setCode(HttpStatus.EXPECTATION_FAILED.value());
             response.setStatus(HttpStatus.EXPECTATION_FAILED.value());
             response.setHeader("status",HttpStatus.EXPECTATION_FAILED.toString());
             return jEntity;
        }

        User user1 = userRepository.findByEmail(user.getEmail());
        if (user1 == null) {
            user1 = new User();
            String encryptedPwd = BCrypt.hashpw(user.getpwd(), BCrypt.gensalt(12));
            user1.setpwd(encryptedPwd);
            user1.setEmail(user.getEmail());
            userRepository.save(user1);


            jEntity.setMsg("User account created successfully!");

            jEntity.setStatuscode(HttpStatus.CREATED);
            jEntity.setCode(HttpStatus.CREATED.value());
            response.setStatus(HttpStatus.CREATED.value());
            response.setHeader("status",HttpStatus.CREATED.toString());
            return jEntity;

        } else {
            jEntity.setMsg("User account already exist!");

            jEntity.setStatuscode(HttpStatus.BAD_REQUEST);
            jEntity.setCode(HttpStatus.BAD_REQUEST.value());
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setHeader("status",HttpStatus.BAD_REQUEST.toString());

            return jEntity;

        }



    }

    @GetMapping(path = "/")
    public @ResponseBody
    JEntity getCurrentTime(HttpServletRequest httpServletRequest,HttpServletResponse response) {

        JEntity j = new JEntity();
        String auth=httpServletRequest.getHeader("Authorization");
        if (auth != null && !auth.isEmpty() && auth.toLowerCase().startsWith("basic")) {
            String base64Credentials = auth.substring("Basic".length()).trim();
            if (!base64Credentials.isEmpty() && base64Credentials!=null &&Base64.isBase64(base64Credentials)) {
                byte[] credDecoded = Base64.decodeBase64(base64Credentials);
                String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                String[] values = credentials.split(":", 2);
                String email = values[0];
                String pwd = values[1];

                User u = userRepository.findByEmail(email);


                if (u == null) {
                    j.setMsg("Please enter a valid email!");

                    j.setStatuscode(HttpStatus.NOT_ACCEPTABLE);
                    j.setCode(HttpStatus.NOT_ACCEPTABLE.value());
                    response.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
                    response.setHeader("status",HttpStatus.NOT_ACCEPTABLE.toString());

                    return j;
                } else {

                    if (!BCrypt.checkpw(pwd, u.getpwd())) {
                        j.setMsg("Please enter valid password!");

                        j.setStatuscode(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS);
                        j.setCode(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS.value());
                        response.setStatus(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS.value());
                        response.setHeader("status",HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS.toString());
                        return j;
                    }
                    Date date=new Date();
                    String strDateFormat= "hh:mm:ss a";
                    DateFormat dateFormat=new SimpleDateFormat(strDateFormat);
                    String formattedDate=dateFormat.format(date);
                    j.setMsg("User is logged in! "+formattedDate);
                    j.setStatuscode(HttpStatus.OK);
                    j.setCode(HttpStatus.OK.value());
                    response.setStatus(HttpStatus.OK.value());
                    response.setHeader("status",HttpStatus.OK.toString());

                    return j;
                }
            }
            else{
                j.setMsg("User is not authorized!");

                j.setStatuscode(HttpStatus.UNAUTHORIZED);
                j.setCode(HttpStatus.UNAUTHORIZED.value());
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setHeader("status",HttpStatus.UNAUTHORIZED.toString());

                return j;
            }


        }
        j.setMsg("User is not logged in!");

        j.setStatuscode(HttpStatus.NOT_FOUND);
        j.setCode(HttpStatus.NOT_FOUND.value());
        response.setStatus(HttpStatus.NOT_FOUND.value());
        response.setHeader("status",HttpStatus.NOT_FOUND.toString());

        return j;
    }

    @PostMapping(path="/note")
    public @ResponseBody Note createNote(@RequestBody Note note,HttpServletRequest httpServletRequest,HttpServletResponse response){
        return saveNote(note,httpServletRequest,response);
    }

    @GetMapping(path="/note/{id}")
    public @ResponseBody Note getNoteWithId(@PathVariable("id") String id,HttpServletRequest httpServletRequest,HttpServletResponse response){
        return getNoteWithIdData(id,httpServletRequest,response);
    }

    @GetMapping(path="/note")
    public @ResponseBody Set<Note> getAllNotes(HttpServletRequest httpServletRequest,HttpServletResponse response){
        return fetchAllNotes(httpServletRequest,response);
    }

    @GetMapping(path="/note/{idNotes}/attachments")
    public @ResponseBody Set<Attachment> getAttachmentsWithNoteId(@PathVariable("idNotes") String id,HttpServletRequest httpServletRequest,HttpServletResponse response){
        return getAttachmentswithNoteIdData(id,httpServletRequest,response);
    }

    @PostMapping("/note/{idNotes}/attachments")
    public @ResponseBody Attachment createFile(@RequestPart(value = "file") MultipartFile file, @PathVariable("idNotes")String noteId, HttpServletRequest httpServletRequest, HttpServletResponse response){
        return saveFile(file,noteId,httpServletRequest,response);
    }

    private Set<Attachment> getAttachmentswithNoteIdData(String noteId, HttpServletRequest httpServletRequest, HttpServletResponse response) {
        String auth=httpServletRequest.getHeader("Authorization");
        StringBuffer msg=new StringBuffer();
        Note note = null;
        Set<Attachment> attachments = null;
        int userid;
        if (auth != null && !auth.isEmpty() && auth.toLowerCase().startsWith("basic")) {
            String base64Credentials = auth.substring("Basic".length()).trim();
            if (!base64Credentials.isEmpty() && base64Credentials!=null &&Base64.isBase64(base64Credentials)) {
                byte[] credDecoded = Base64.decodeBase64(base64Credentials);
                String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                String[] values = credentials.split(":", 2);
                String email = values[0];
                String pwd = values[1];
                // request.
                User user1 = userRepository.findByEmail(email);


                if (user1 == null) {

                    msg.append("Email is invalid");
                    setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                    return attachments;

                } else {
                    if (!BCrypt.checkpw(pwd, user1.getpwd())) {
                        msg.append("Password is incorrect");
                        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                        return attachments;
                    }
                    note = noteRepository.findById(noteId);


                    if (note == null){
                        msg.append("Note not found");
                        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                        return attachments;
                    }
                    else {
                        attachments = note.getAttachments();
                        if(attachments == null) {
                            msg.append("No attachments for this note");
                            setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                            return attachments;
                        } else {
                            setResponse(HttpStatus.OK,response);
                            return attachments;
                        }
                    }
                }
            }
            else{

                msg.append("User is not logged in");
                setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                return attachments;
            }


        }
        msg.append("User is not logged in");
        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
        return attachments;

    }

    public Attachment saveFile(MultipartFile file,String noteId,HttpServletRequest httpServletRequest, HttpServletResponse response){
        System.out.println("Active profileName:" + profileName);
        String auth = httpServletRequest.getHeader("Authorization");
        StringBuffer msg = new StringBuffer();
        Note note = null;

        Attachment a = null;

        String mimeType = file.getContentType();
        String type = mimeType.split("/")[0];
        if (!type.equalsIgnoreCase("image")) {
            msg.append("Only Images allowed");
            setResponse(HttpStatus.UNAUTHORIZED, response, msg);
            return a;
        }


        if (auth != null && !auth.isEmpty() && auth.toLowerCase().startsWith("basic")) {
            String base64Credentials = auth.substring("Basic".length()).trim();
            if (!base64Credentials.isEmpty() && base64Credentials != null && Base64.isBase64(base64Credentials)) {
                byte[] credDecoded = Base64.decodeBase64(base64Credentials);
                String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                String[] values = credentials.split(":", 2);
                String email = values[0];
                String pwd = values[1];
                // request.
                User user = userRepository.findByEmail(email);

                if (user == null) {

                    msg.append("Email is invalid");
                    setResponse(HttpStatus.UNAUTHORIZED, response, msg);
                    return a;

                } else {
                    if (!BCrypt.checkpw(pwd, user.getpwd())) {
                        msg.append("Password is incorrect");
                        setResponse(HttpStatus.UNAUTHORIZED, response, msg);
                        return a;
                    }
                    note = noteRepository.findById(noteId);

                    if (note == null) {
                        msg.append("No such Note available");
                        setResponse(HttpStatus.UNAUTHORIZED, response, msg);
                        return a;
                    }
                    else {
                        String url=null;
                        if(profileName.equalsIgnoreCase("dev")){
                            url=uploadToAWS(file);

                        }
                        else
                        {
                            url=uploadToFileSystem(file);

                        }
                        a=createAttachment(file,note,url);
                        attachmentRepository.save(a);
                        return a;
                    }
                }
            } else{

                msg.append("User is not logged in");
                setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                return a;
            }

        }
        msg.append("User is not logged in");
        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
        return a;
    }

    public String uploadToAWS(MultipartFile multipartFile) {

        String fileUrl = "";
        try {


            File file = convertMultiPartFileToFile(multipartFile);
            String fileName = multipartFile .getOriginalFilename();
            String endpointUrl=env.getProperty("endpointurl");
            String bucketName=env.getProperty("bucketname");
            fileUrl = endpointUrl + "/" + bucketName + "/" + fileName;
            amazonClient.uploadFileTos3bucket(bucketName,fileName, file);

            // file.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileUrl;
    }

    public File convertMultiPartFileToFile(MultipartFile file) throws IOException {
        File convFile = new File(file.getOriginalFilename());
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(file.getBytes());
        fos.close();
        return convFile;
    }

    public Attachment createAttachment(MultipartFile file,Note n,String url){
        // Note n=new Note();
       // String url=uploadToFileSystem(file);

        Attachment a=new Attachment();

        // String p=env.getProperty("uploadpath");
        //  String fileName = fileStorageService.storeFile(file,p);

        a.setUrl(url);
        a.setNote(n);
        return a;
    }

    public String uploadToFileSystem(MultipartFile file){
        // System.out.println(profilename);

        Path path=null;
        try {
            // Get the file and save it somewhere
            byte[] bytes = file.getBytes();
            path = Paths.get(env.getProperty("uploadpath") + file.getOriginalFilename());
            Files.write(path, bytes);
            return path.toString();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return path.toString();
    }

    @DeleteMapping  (path="/note/{id}/attachments/{idAttachments}")
    public @ResponseBody Object deleteAttachment(@PathVariable("id") String id,@PathVariable("idAttachments") String idAttachments,HttpServletRequest httpServletRequest,HttpServletResponse response){
        return deleteAttachmentWithNoteId(id, idAttachments, httpServletRequest, response);

    }

    private Object deleteAttachmentWithNoteId(String noteId, String idAttachments, HttpServletRequest httpServletRequest, HttpServletResponse response) {

        String auth=httpServletRequest.getHeader("Authorization");
        StringBuffer msg=new StringBuffer();
        Note note = null;
        //Set<Attachment> attachments = null;
        Attachment attachment = null;
        if (auth != null && !auth.isEmpty() && auth.toLowerCase().startsWith("basic")) {
            String base64Credentials = auth.substring("Basic".length()).trim();
            if (!base64Credentials.isEmpty() && base64Credentials!=null &&Base64.isBase64(base64Credentials)) {
                byte[] credDecoded = Base64.decodeBase64(base64Credentials);
                String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                String[] values = credentials.split(":", 2);
                String email = values[0];
                String pwd = values[1];


                User user1 = userRepository.findByEmail(email);

                if (user1 == null) {

                    msg.append("Email is Invalid");
                    setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                    return attachment;


                } else {
                    if (!BCrypt.checkpw(pwd, user1.getpwd())) {
                        msg.append("Password is Invalid");
                        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                        return attachment;

                    }
                    note = noteRepository.findById(noteId);
                    if (note == null)
                    {


                        msg.append("Note not found");
                        setResponse(HttpStatus.BAD_REQUEST,response,msg);
                        return attachment;

                    }
                    else {

                        if (idAttachments == null)
                        {
                            msg.append("attachment not found");
                            setResponse(HttpStatus.BAD_REQUEST,response,msg);
                            return attachment;
                        }
                        else
                        {
                            attachment=attachmentRepository.findById(idAttachments);
                            if(attachment == null)
                            {


                                msg.append("Note not found");
                                setResponse(HttpStatus.BAD_REQUEST,response,msg);
                                return attachment;

                            }
                            else {
                                if (profileName.equalsIgnoreCase("dev")) {
                                    //deleteFromAWS(file);

                                    if (!(attachmentRepository.findById(idAttachments).getNote() == note)){
                                        msg.append("This attachment is not entitled to the given note");
                                        setResponse(HttpStatus.UNAUTHORIZED, response, msg);
                                        return attachmentRepository.findById(idAttachments);



                                    }
                                    else{
                                    String bucketName = env.getProperty("bucketname");

                                    String fileName = attachment.getUrl();
                                    amazonClient.deleteFileFromS3Bucket(bucketName,fileName);
                                    msg.append("Deleted Successfully from S3");
                                    setResponse(HttpStatus.OK,response,msg);
                                    return attachment;
                                    }
                                } else {


                                    //a = createAttachment(file, note);
                                    if (note.getUser().getId() == user1.getId()) {

                                        Instant ins = Instant.now();
                                        if (!(attachmentRepository.findById(idAttachments).getNote() == note)){
                                            msg.append("This attachment is not entitled to the given note");
                                            setResponse(HttpStatus.UNAUTHORIZED, response, msg);
                                            return attachmentRepository.findById(idAttachments);



                                        }
                                        else {
                                            note.setUpdated_on(ins.toString());
                                            attachmentRepository.delete(attachment);
                                            File destFile = new File(attachment.getUrl());
                                            if (destFile.exists()) {
                                                destFile.delete();
                                            }
                                            msg.append("Deleted Successfully from local file system");
                                            setResponse(HttpStatus.NO_CONTENT, response, msg);
                                            return null;
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
            }
            else{
                msg.append("You are not Authorized to use this note");
                setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                return attachment;
            }


        }
        // j.setMsg("User is not logged in!");
        msg.append("You are not Authorized to use this note");
        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
        return attachment;
    }


    @PutMapping("/note/{idNotes}/attachments/{idAttachments}")
    public @ResponseBody Attachment updateFile(@RequestPart(value = "file") MultipartFile file, @PathVariable("idNotes")String noteId,@PathVariable("idAttachments")String attachmentId, HttpServletRequest httpServletRequest, HttpServletResponse response){
        return editFile(file,noteId,attachmentId,httpServletRequest,response);
    }





    public Attachment editFile(MultipartFile file,String noteId,String attachmentId,HttpServletRequest httpServletRequest, HttpServletResponse response){
//        String profileName="default";
        String auth = httpServletRequest.getHeader("Authorization");
        StringBuffer msg = new StringBuffer();
        Note note = null;

        Attachment a = null;

        String mimeType = file.getContentType();
        String type = mimeType.split("/")[0];
        if (!type.equalsIgnoreCase("image")) {
            msg.append("Only Images allowed");
            setResponse(HttpStatus.UNAUTHORIZED, response, msg);
            return a;
        }


        if (auth != null && !auth.isEmpty() && auth.toLowerCase().startsWith("basic")) {
            String base64Credentials = auth.substring("Basic".length()).trim();
            if (!base64Credentials.isEmpty() && base64Credentials != null && Base64.isBase64(base64Credentials)) {
                byte[] credDecoded = Base64.decodeBase64(base64Credentials);
                String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                String[] values = credentials.split(":", 2);
                String email = values[0];
                String pwd = values[1];
                // request.
                User user = userRepository.findByEmail(email);

                if (user == null) {

                    msg.append("Email is invalid");
                    setResponse(HttpStatus.UNAUTHORIZED, response, msg);
                    return a;

                } else {
                    if (!BCrypt.checkpw(pwd, user.getpwd())) {
                        msg.append("Password is incorrect");
                        setResponse(HttpStatus.UNAUTHORIZED, response, msg);
                        return a;
                    }
                    note = noteRepository.findById(noteId);

                    if (note == null) {
                        msg.append("No such Note available");
                        setResponse(HttpStatus.UNAUTHORIZED, response, msg);
                        return a;
                    }

                    else {
                        if(profileName.equalsIgnoreCase("dev")){

                            String bucketName = env.getProperty("bucketname");
                            Attachment a2 = attachmentRepository.findById(attachmentId);

                               if (!(a2.getNote() == note)){
                                msg.append("This attachment is not entitled to the given note");
                                setResponse(HttpStatus.UNAUTHORIZED, response, msg);
                                return attachmentRepository.findById(attachmentId);



                            }
                               else {

                                   String fileName = a2.getUrl();
                                   amazonClient.deleteFileFromS3Bucket(bucketName, fileName);
                                   msg.append("Deleted Successfully from local file system");
                                   setResponse(HttpStatus.NO_CONTENT, response, msg);
                                   uploadToAWS(file);
                                   return null;
                               }

                        }
                        else
                        {
                            Attachment a1 = attachmentRepository.findById(attachmentId);

                            if (a1 == null)
                            {
                                msg.append("No such attachment available");
                                setResponse(HttpStatus.UNAUTHORIZED, response, msg);
                                return a1;



                            }
//                            else if (!((attachmentRepository.findById(attachmentId).getNote() == note))) {
//                                msg.append("This attachment is not entitled to the given note");
//                                setResponse(HttpStatus.UNAUTHORIZED, response, msg);
//                                return attachmentRepository.findById(attachmentId);
//
//                            }
                            else if (!(a1.getNote() == note)){
                                msg.append("This attachment is not entitled to the given note");
                                setResponse(HttpStatus.UNAUTHORIZED, response, msg);
                                return attachmentRepository.findById(attachmentId);



                            }


                            else {
                                //attachmentRepository.delete(a1);
                                File destFile = new File(a1.getUrl());
                                if(destFile.exists()){
                                    destFile.delete();
                                }
                                String url=uploadToFileSystem(file);
                                a1.setUrl(url);
                              //  setResponse(resp);
                                setResponse(HttpStatus.NO_CONTENT, response, msg);

                                // a = createAttachment(file, note);
                                //   attachmentRepository.save(a1);
                                return null;
                            }
                        }

                    }
                }
            } else{

                msg.append("User is not logged in");
                setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                return a;
            }

        }
        msg.append("User is not logged in");
        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
        return a;
    }



    public Note saveNote(Note note,HttpServletRequest httpServletRequest,HttpServletResponse response){
        String auth=httpServletRequest.getHeader("Authorization");
        StringBuffer msg=new StringBuffer();
        Note n=null;
        if (auth != null && !auth.isEmpty() && auth.toLowerCase().startsWith("basic")) {
            String base64Credentials = auth.substring("Basic".length()).trim();
            if (!base64Credentials.isEmpty() && base64Credentials!=null &&Base64.isBase64(base64Credentials)) {
                byte[] credDecoded = Base64.decodeBase64(base64Credentials);
                String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                String[] values = credentials.split(":", 2);
                String email = values[0];
                String pwd = values[1];
                // request.
                User u = userRepository.findByEmail(email);


                if (u == null) {
                    msg.append("Email is invalid");
                    setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                    return n;

                } else {


                    if (!BCrypt.checkpw(pwd, u.getpwd())) {
                        msg.append("Password is incorrect");
                        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                        return n;

                    }
                    if (note==null){
                        msg.append("Please enter title and content for note");
                        setResponse(HttpStatus.BAD_REQUEST,response,msg);
                        return n;
                    }
                    if (note.getContent()==null || note.getTitle()==null){
                        msg.append("Please enter title and content for note");
                        setResponse(HttpStatus.BAD_REQUEST,response,msg);
                        return n;
                    }
                    n=createNote(u,note);
                    noteRepository.save(n);
                    setResponse(HttpStatus.CREATED,response);

                    return n;

                }
            }
            else{

                msg.append("User is not logged in");
                setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                return n;
            }


        }
        msg.append("User is not logged in");
        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
        return n;
    }


    public Note createNote(User u,Note note){
        Note n=new Note();
        Instant ins=Instant.now();


      //  n.setId(UUID.randomUUID().toString());
        n.setCreated_on(ins.toString());
        n.setUpdated_on(ins.toString());
        n.setTitle(note.getTitle());
        n.setContent(note.getContent());
        n.setUser(u);
        return n;
    }

    public void setResponse(HttpStatus hs,HttpServletResponse response){

        response.setStatus(hs.value());
        response.setHeader("status", hs.toString());
    }

    public void setResponse(HttpStatus hs,HttpServletResponse response,StringBuffer message){
        response.setStatus(hs.value());
        response.setHeader("status", hs.toString());
        try {
            response.sendError(hs.value(),message.toString());
        }
        catch(Exception e){

        }

    }
    public Note getNoteWithIdData(String id,HttpServletRequest httpServletRequest,HttpServletResponse response){
        String auth=httpServletRequest.getHeader("Authorization");
        StringBuffer msg=new StringBuffer();
        Note n=null;
        if (auth != null && !auth.isEmpty() && auth.toLowerCase().startsWith("basic")) {
            String base64Credentials = auth.substring("Basic".length()).trim();
            if (!base64Credentials.isEmpty() && base64Credentials!=null &&Base64.isBase64(base64Credentials)) {
                byte[] credDecoded = Base64.decodeBase64(base64Credentials);
                String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                String[] values = credentials.split(":", 2);
                String email = values[0];
                String pwd = values[1];
                // request.
                User u = userRepository.findByEmail(email);


                if (u == null) {

                    msg.append("Email is invalid");
                    setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                    return n;

                } else {


                    if (!BCrypt.checkpw(pwd, u.getpwd())) {
                        msg.append("Password is incorrect");
                        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                        return n;

                    }
                    n=noteRepository.findById(id);

                    if (n==null){
                        msg.append("Note could not be found. Please enter a valid note id");
                        setResponse(HttpStatus.NOT_FOUND,response,msg);
                        return n;
                    }

                    if(n.getUser().getId()!=u.getId()){
                        msg.append("User is not authorized to use this note");
                        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                        return null;
                    }

                    setResponse(HttpStatus.OK,response);
                    return n;

                }
            }
            else{

                msg.append("User is not logged in");
                setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                return n;
            }


        }
        msg.append("User is not logged in");
        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
        return n;
    }

    public Set<Note> fetchAllNotes(HttpServletRequest httpServletRequest, HttpServletResponse response){
        String auth=httpServletRequest.getHeader("Authorization");
        StringBuffer msg=new StringBuffer();
        Set<Note> n=null;
        int userid;
        if (auth != null && !auth.isEmpty() && auth.toLowerCase().startsWith("basic")) {
            String base64Credentials = auth.substring("Basic".length()).trim();
            if (!base64Credentials.isEmpty() && base64Credentials!=null &&Base64.isBase64(base64Credentials)) {
                byte[] credDecoded = Base64.decodeBase64(base64Credentials);
                String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                String[] values = credentials.split(":", 2);
                String email = values[0];
                String pwd = values[1];
                // request.
                User u = userRepository.findByEmail(email);


                if (u == null) {

                    msg.append("Email is invalid");
                    setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                    return n;

                } else {
                    if (!BCrypt.checkpw(pwd, u.getpwd())) {
                        msg.append("Password is incorrect");
                        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                        return n;
                    }

                    n = u.getLstNote();

                    if (n == null){
                        msg.append("Notes could not be found for this user");
                        setResponse(HttpStatus.NOT_FOUND,response,msg);
                        return n;
                    }
                    if (n.isEmpty()){
                        msg.append("Notes could not be found for this user");
                        setResponse(HttpStatus.NOT_FOUND,response,msg);
                        return null;
                    }
//

                    setResponse(HttpStatus.OK,response);
                    return n;

                }
            }
            else{

                msg.append("User is not logged in");
                setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                return n;
            }


        }
        msg.append("User is not logged in");
        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
        return n;
    }


    @PutMapping (path="/note/{id}")
    public @ResponseBody Object upateNote(@RequestBody Note note, @PathVariable("id") String id,HttpServletRequest httpServletRequest,HttpServletResponse response){
        //JEntity j = new JEntity();
        String auth=httpServletRequest.getHeader("Authorization");
        StringBuffer msg=new StringBuffer();
        Note n=null;
        if (auth != null && !auth.isEmpty() && auth.toLowerCase().startsWith("basic")) {
            String base64Credentials = auth.substring("Basic".length()).trim();
            if (!base64Credentials.isEmpty() && base64Credentials!=null &&Base64.isBase64(base64Credentials)) {
                byte[] credDecoded = Base64.decodeBase64(base64Credentials);
                String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                String[] values = credentials.split(":", 2);
                String email = values[0];
                String pwd = values[1];
                JEntity j =new JEntity();


                User u = userRepository.findByEmail(email);


                if (u == null) {

                    msg.append("Email is Invalid");
                    setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                    return n;


                } else {
                    if (!BCrypt.checkpw(pwd, u.getpwd())) {
                        msg.append("Password is Invalid");
                        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                        return n;

                    }



                    Note n1 = noteRepository.findById(id);
                    System.out.println("n1:"+n1);
                    if (n1 == null)
                    {


                        msg.append("Note not found");
                        setResponse(HttpStatus.BAD_REQUEST,response,msg);
                        return n1;

                    }
                    else {

                        if (n1.getUser().getId() == u.getId()) {

                            if (note.getContent()==null || note.getTitle()==null){
                                msg.append("Please enter title and content for note");
                                setResponse(HttpStatus.BAD_REQUEST,response,msg);
                                return null;
                            }

//                        Note n = new Note();
                            Instant ins = Instant.now();
//                        n.setId(UUID.randomUUID().toString());
//
//                        n.setCreated_on(ins.toString());
                            n1.setUpdated_on(ins.toString());
                            n1.setTitle(note.getTitle());
                            n1.setContent(note.getContent());

                            noteRepository.save(n1);
                            setResponse(HttpStatus.NO_CONTENT,response);
                            return null;



                        } else {

                            msg.append("You are not Authorized to use this note");
                            setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                            return n1;
                        }
                    }

                }
            }
            else{
                msg.append("You are not Authorized to use this note");
                setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                return n;
            }


        }
        // j.setMsg("User is not logged in!");
        msg.append("You are not Authorized to use this note");
        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
        return n;
    }



    @DeleteMapping  (path="/note/{id}")
    public @ResponseBody Object deleteNote(@PathVariable("id") String id,HttpServletRequest httpServletRequest,HttpServletResponse response){
        //JEntity j = new JEntity();
        String auth=httpServletRequest.getHeader("Authorization");
        StringBuffer msg=new StringBuffer();
        Note n=null;
        if (auth != null && !auth.isEmpty() && auth.toLowerCase().startsWith("basic")) {
            String base64Credentials = auth.substring("Basic".length()).trim();
            if (!base64Credentials.isEmpty() && base64Credentials!=null &&Base64.isBase64(base64Credentials)) {
                byte[] credDecoded = Base64.decodeBase64(base64Credentials);
                String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                String[] values = credentials.split(":", 2);
                String email = values[0];
                String pwd = values[1];
                JEntity j =new JEntity();


                User u = userRepository.findByEmail(email);


                if (u == null) {

                    msg.append("Email is Invalid");
                    setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                    return n;


                } else {
                    if (!BCrypt.checkpw(pwd, u.getpwd())) {
                        msg.append("Password is Invalid");
                        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                        return n;

                    }
                    Note n1 = noteRepository.findById(id);
                    System.out.println("n1:"+n1);
                    if (n1 == null)
                    {


                        msg.append("Note not found");
                        setResponse(HttpStatus.BAD_REQUEST,response,msg);
                        return n1;

                    }
                    else {

                        if (n1.getUser().getId() == u.getId()) {

                            Instant ins = Instant.now();

                            n1.setUpdated_on(ins.toString());

                            noteRepository.delete(n1);
                            setResponse(HttpStatus.NO_CONTENT, response);
                            return null;
                        }
                    }
                }
            }
            else{
                msg.append("You are not Authorized to use this note");
                setResponse(HttpStatus.UNAUTHORIZED,response,msg);
                return n;
            }


        }
        // j.setMsg("User is not logged in!");
        msg.append("You are not Authorized to use this note");
        setResponse(HttpStatus.UNAUTHORIZED,response,msg);
        return n;
    }

    public static boolean validateEmail(String emailStr) {
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX .matcher(emailStr);
        return matcher.find();
    }
    public static boolean validatePwd(String pwdStr) {
        Matcher matcher = VALID_PWD_REGEX.matcher(pwdStr);
        return matcher.find();
    }



}
