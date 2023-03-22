package org.jlab.resize;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import org.jlab.resize.util.IOUtil;

/**
 *
 * Max File Size: 15MB Max Request Size: 20MB However application server max
 * post size is likely much lower (maybe 6MB)
 *
 * @author ryans
 */
@WebServlet(name = "ConverterController", urlPatterns = {"/convert"})
@MultipartConfig(maxFileSize = 15728640, maxRequestSize = 20971520)
public class ConverterController extends HttpServlet {

    private final static Logger logger = Logger.getLogger(ConverterController.class.getName());
    // Careful with these as they are shared among multiple threads!
    private static final String TMP_FILE_PREFIX = "imagemagic";
    private static final long MAX_EXECUTE_MILLIS = 45000;
    private File tmpDir;
    private String execPath;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        execPath = config.getServletContext().getInitParameter("mogrify");

        if (execPath == null || execPath.isEmpty()) {
            execPath = System.getenv("mogrify");

            if (execPath == null || execPath.isEmpty()) {
                throw new ServletException("Path to convert executable must be specified in an environment variable 'mogrify' or as a web application context parameter");
            }
        }

        String tmpDirPath = config.getServletContext().getInitParameter("tmpdir");

        if (tmpDirPath == null || tmpDirPath.isEmpty()) {
            tmpDirPath = System.getenv("tmpdir");

            if (tmpDirPath == null || tmpDirPath.isEmpty()) {
                tmpDirPath = System.getProperty("java.io.tmpdir");
            }
        }

        logger.log(Level.FINE, "tmpDirPath: {0}", tmpDirPath);
        logger.log(Level.FINE, "execPath: {0}", execPath);

        File exec = new File(execPath);

        if (!exec.exists()) {
            throw new ServletException("Executable mogrify does not exist");
        }

        tmpDir = new File(tmpDirPath);

        if (!tmpDir.exists()) {
            throw new ServletException("Temporary directory does not exist");
        }

        if (!tmpDir.canRead()) {
            throw new ServletException("Temporary directory permissions prevent read");
        }

        if (!tmpDir.canWrite()) {
            throw new ServletException("Temporary directory permissions prevent write");
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Part filePart = request.getPart("file");
        ImageMeta meta = convertAndValidateMeta(filePart);
        Dimension size = convertAndValidateSize(request);
        boolean allowStretching = convertAndValidateBoolean(request, "allow-stretching");
        boolean ignoreAspectRatio = convertAndValidateBoolean(request, "ignore-aspect-ratio");
        boolean outputDataUri = convertAndValidateBoolean(request, "output-data-uri");
        boolean forcePng = convertAndValidateBoolean(request, "force-png");

        logger.log(Level.FINE, "Uploaded Filename: {0}", meta.getFilename());
        logger.log(Level.FINE, "Mime Type: {0}", meta.getType().getMime());
        logger.log(Level.FINE, "Requested Dimension: {0}x{1}", new Object[]{size.getWidth(), size.getHeight()});
        logger.log(Level.FINE, "Allow Stretching: {0}", allowStretching);
        logger.log(Level.FINE, "Ignore Aspect Ratio: {0}", ignoreAspectRatio);
        logger.log(Level.FINE, "Output Data URI: {0}", outputDataUri);
        logger.log(Level.FINE, "Force PNG: {0}", forcePng);
        
        File file = File.createTempFile(TMP_FILE_PREFIX, meta.getType().getExtension(), tmpDir);
        String outPath = file.getPath();
        ImageMeta outMeta = meta;
        
        if (forcePng) {
            outPath = outPath.substring(0, outPath.lastIndexOf(meta.getType().getExtension())) + ".png";
            outMeta = new ImageMeta(ImageType.PNG, meta.getFilename().substring(0, meta.getFilename().lastIndexOf(".")) + ".png");         
        }

        File outfile = null;

        InputStream in = null;
        OutputStream out = null;

        try {
            String imagePath = file.getPath();

            try {
                in = filePart.getInputStream();
                out = new FileOutputStream(file);

                IOUtil.copy(in, out);
            } finally {
                IOUtil.closeQuietly(in);
                IOUtil.closeQuietly(out);
            }

            String shrinkOption = ">";

            if (allowStretching) {
                shrinkOption = "";
            }

            String aspectRatioOption = "";

            if (ignoreAspectRatio) {
                aspectRatioOption = "!";
            }

            List<String> command = new ArrayList<String>();
            command.add(execPath);
            if (forcePng) {
                command.add("-format");
                command.add("png");
                command.add("-auto-orient");
            }
            command.add("-resize");
            command.add(size.width + "x" + size.height + aspectRatioOption + shrinkOption);
            command.add(imagePath);

            ProcessBuilder builder = new ProcessBuilder(command);

            /*String c = "";

            for (String s : command) {
                c = c + " " + s;
            }

            logger.log(Level.FINEST, "Command: {0}", c);*/

            builder.redirectErrorStream(true);

            Timer timer = new Timer();
            timer.schedule(new InterruptTimerTask(Thread.currentThread()), MAX_EXECUTE_MILLIS);
            Process p = builder.start();
            Thread t = new Thread(new StreamGobbler(p.getInputStream()));
            t.start();

            try {
                int status = p.waitFor();

                if (status != 0) {
                    throw new ServletException("Unexpected status from ImageMagick process: " + status);
                }
            } catch (InterruptedException e) {
                p.destroy();
                throw new ServletException("Interrupted while waiting for ImageMagick", e);
            } finally {
                // If task completes without interruption we must cancel the 
                // interrupt task to prevent interrupt later on!
                timer.cancel();

                // Clear interrupted flag for two cases:
                // (1) task completed but timer task sets interrupt flag before
                // we can cancel it
                // (2) task isn't completed and is interrupted by timer task; note 
                // that most things in Java clear the interrupt flag before throwing
                // and exception, but Process.waitFor does not;
                // see http://bugs.sun.com/view_bug.do?bug_id=6420270
                Thread.interrupted();
            }

            out = response.getOutputStream();

            in = null;

            // If we made it this far then assume image resized and 
            // nothing is buffered in response 
            try {
                outfile = new File(outPath);
                
                if (outputDataUri) {
                    response.setContentType("text/plain;charset=UTF-8");

                    byte[] imageBytes = IOUtil.fileToBytes(outfile);

                    String imageString = IOUtil.encodeBase64(imageBytes);

                    StringBuilder strbldr = new StringBuilder();
                    strbldr.append("data:");
                    strbldr.append(outMeta.type.mime);
                    strbldr.append(";base64,");
                    strbldr.append(imageString);

                    String dataURI = strbldr.toString();

                    in = new ByteArrayInputStream(dataURI.getBytes("UTF-8"));

                    IOUtil.copy(in, out);
                } else {
                    response.setContentType(outMeta.getType().getMime());
                    response.setHeader("Content-Disposition", "attachment; filename=" + outMeta.getFilename());

                    in = new FileInputStream(outfile);

                    IOUtil.copy(in, out);
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Unable to copy steam", e); // too late to send error to client; headers already sent
            } finally {
                IOUtil.closeQuietly(in);
            }
        } finally {
            if (outfile != null && !outfile.equals(file)) {
                boolean outsuccess = outfile.delete();

                if (!outsuccess) {
                    logger.log(Level.WARNING, "Unable to delete temporary file: {0}", outfile.getName());
                }
            }

            boolean success = file.delete();

            if (!success) {
                logger.log(Level.WARNING, "Unable to delete temporary file: {0}", file.getName());
            }
        }
    }

    private String getFilename(Part part) {
        for (String cd : part.getHeader("content-disposition").split(";")) {
            if (cd.trim().startsWith("filename")) {
                String filename = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                return filename.substring(filename.lastIndexOf('/') + 1).substring(filename.lastIndexOf('\\') + 1); // MSIE fix.
            }
        }
        return null;
    }

    private ImageMeta convertAndValidateMeta(Part part) throws ServletException {
        String filename = getFilename(part);
        String contentType = part.getContentType();
        ImageType type;

        if ("image/png".equals(contentType)) {
            type = ImageType.PNG;
        } else if ("image/gif".equals(contentType)) {
            type = ImageType.GIF;
        } else if ("image/jpeg".equals(contentType) || "image/jpg".equals(contentType)) {
            type = ImageType.JPEG;
        } else {
            throw new ServletException("Content Type must be one of image/png, image/jpeg, image/gif");
        }

        return new ImageMeta(type, filename);
    }

    private boolean convertAndValidateBoolean(HttpServletRequest request, String name) {
        String valueStr = request.getParameter(name);
        return (valueStr != null);
    }

    private Integer convertAndValidateInt(HttpServletRequest request, String name) {
        String valueStr = request.getParameter(name);
        Integer value = null;

        if (valueStr != null && !valueStr.isEmpty()) {
            value = Integer.valueOf(valueStr);
        }

        return value;
    }

    private Dimension convertAndValidateSize(HttpServletRequest request) throws ServletException {
        Integer width = convertAndValidateInt(request, "width");
        Integer height = convertAndValidateInt(request, "height");

        if (width == null) {
            throw new ServletException("width cannot be empty");
        }

        if (height == null) {
            throw new ServletException("height cannot be empty");
        }

        return new Dimension(width, height);
    }

    private class StreamGobbler implements Runnable {

        private InputStream in;

        StreamGobbler(InputStream in) {
            this.in = in;
        }

        @Override
        public void run() {
            try {
                InputStreamReader reader = new InputStreamReader(in);
                BufferedReader buffer = new BufferedReader(reader);
                String line = null;
                while ((line = buffer.readLine()) != null) {
                    logger.log(Level.FINE, line);
                }
            } catch (IOException ioe) {
                logger.log(Level.SEVERE, "Unable to gobble stream", ioe);
            }
        }
    }

    private class InterruptTimerTask extends TimerTask {

        private Thread thread;

        public InterruptTimerTask(Thread t) {
            this.thread = t;
        }

        @Override
        public void run() {
            thread.interrupt();
        }
    }

    private class ImageMeta {

        private ImageType type;
        private String filename;

        public ImageMeta(ImageType type, String filename) {
            this.type = type;
            this.filename = filename;
        }

        public ImageType getType() {
            return type;
        }

        public String getFilename() {
            return filename;
        }
    }

    private enum ImageType {

        JPEG(".jpeg", "image/jpeg"),
        GIF(".gif", "image/gif"),
        PNG(".png", "image/png");
        private String extension;
        private String mime;

        ImageType(String extension, String mime) {
            this.extension = extension;
            this.mime = mime;
        }

        public String getExtension() {
            return extension;
        }

        public String getMime() {
            return mime;
        }
    }
}
