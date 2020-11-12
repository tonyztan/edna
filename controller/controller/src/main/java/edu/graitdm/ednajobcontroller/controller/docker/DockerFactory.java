package edu.graitdm.ednajobcontroller.controller.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.core.command.PushImageResultCallback;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.hubspot.jinjava.Jinjava;
import edu.graitdm.ednajobcontroller.configuration.BaseConfiguration;
import edu.graitdm.ednajobcontroller.controller.ednajob.EdnaJob;
import okio.Options;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

//import static org.graalvm.compiler.debug.DebugOptions.PrintGraphTarget.File;

public class DockerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DockerFactory.class);

    private final DockerClient dockerClient;
    private final BaseConfiguration configuration;

    public DockerFactory(DockerClient dockerClient, BaseConfiguration configuration){
        this.dockerClient = dockerClient;
        this.configuration = configuration;
    }

    public void add(EdnaJob ednaJob) {
        /**
         * Here, we do what generate_job_python.bash does...
         * Steps:
         * Extract docker context from the ednajob
         *      context is located at /homedir/ednajob.applicationname/ednajob.jobname
         *      homedir is set in the baseconfiguration (or passed as command line arg)
         *      basically it's the path where the jobs are, e.g. /local/path/to/repo/examples/
         * Extract edna source directory from the baseconfiguration
         *      basically, path to /local/path/to/repo/python/edna/
         *      From here, we can copy the crd folder, setup.cfg, and setup.py...
         *      TODO(Abhijit) FUTURE, adjust this so that user defaults to pypi
         *          (and can switch to a local pypi server hosting updated development version?)
         * Use jinja2 to generate a dockerfile and populated it with the correct variables extracted from
         *      ednajob. (https://github.com/HubSpot/jinjava) NOTE:
         *          template.filename = ednajob.spec.filename
         *          template.jobcontext = ednajob.spec.jobcontext
         *          template.filedependencies = ednajob.spec.filedependencies
         *              filedependencies is a 'list' of additional dependencies for the edna library
         *              'list', not list because we will have uses just pass files as a string separated by space
         *              i.e.:
         *                   filedependencies: file1.py file2.txt file3.zip
         *              not yet tested :/ but shouldn't matter. leave null in jinja template for now
         *           template.jobdependencies = ednajob.spec.jobdependencies
         *              this is the installation dependency for the edna library
         *              primary for python; used currently if someone wants mysql or sklearn...(see part 7)
         *              if this is not specified, no need to add it to jinja template, because Dickerfile.jinja
         *              handles nulll values
         * Copy that generated Dockerfile to the context
         * Generate a dockerignore (use the dockerignore in resources?) and copy to the context
         * Copy the edna source files to the context
         * Build the image (https://www.baeldung.com/docker-java-api)
         * Tag the image (https://www.baeldung.com/docker-java-api)
         * Push the image (https://www.baeldung.com/docker-java-api)
         * Delete source files in the context (would this cause too much IO overhead? probably not)
         * Delete the dockerignore in the context
         * Delete the Dockerfile in the context
         * DONE -- at thisi point, control returns to EdnaJobController, which checks for namespace and creates deployment, etc...
         */

//        * Extract docker context from the ednajob
//        *      context is located at /homedir/ednajob.applicationname/ednajob.jobname
//        *      homedir is set in the baseconfiguration (or passed as command line arg)
//        *      basically it's the path where the jobs are, e.g. /local/path/to/repo/examples/

        // Get the docker context (will need to combine a bunch of strings and then convert to path); see above
        String pathString = configuration.getEdnaAppdir() +
                "/" + ednaJob.getSpec().getApplicationname() +
                "/" + ednaJob.getSpec().getJobname();

        Path context = Paths.get(pathString);

        // Get the edna source path from the configuration.ednasourcepath...
        Path ednaSource = Paths.get(configuration.getEdnaSourcedir());

        // Build the jinja2 context from ednaJob; see https://github.com/HubSpot/jinjava.
        Map<String,String> jinjaContext = new HashMap<String, String>();
        jinjaContext.put("filename", ednaJob.getSpec().getFilename());
        jinjaContext.put("jobcontext", ednaJob.getSpec().getJobcontext());
        jinjaContext.put("filedependencies", ednaJob.getSpec().getFiledependencies());

        // Extract the jinja2 template from the resources directory with Resources.toString (see see https://github.com/HubSpot/jinjava)
        String template = "";
        try {
            template = Resources.toString(Resources.getResource("Dockerfile.jinja2"), Charsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Use jinja2 to create the Dockerfile; see https://github.com/HubSpot/jinjava.
        Jinjava jinjava = new Jinjava();
        // (rendering the dockerfile)
        String renderedDockerfile = jinjava.render(template, jinjaContext);

        /*
        Files.copy(source, target, Options)  // https://docs.oracle.com/javase/tutorial/essential/io/copy.html
         */

//        String dockerignore = "# Ignore .git and .cache\n" +
//                ".git\n" +
//                ".cache\n" +
//                "\n" +
//                "# Ignore config yaml files and generated docker.sh file\n" +
//                "config.yaml\n" +
//                "deployment.yaml\n" +
//                "docker.sh";

        try {
            //Save the renderedDockerfile to context/Dockerfile
            Files.writeString(context.resolve("Dockerfile"), renderedDockerfile, Charsets.UTF_8, StandardOpenOption.CREATE);

            // Generate a dockerignore (i.e. just copy is from the resources folder); NOTE -- do we even need a dockerignore anymore???
            // Files.writeString(context.resolve(".dockerignore"), dockerignore, Charsets.UTF_8);

            Path srcDockerIgnore = Paths.get(Resources.getResource(".dockerignore").toString());
            Files.copy(srcDockerIgnore, context.resolve(".dockerignore"), StandardCopyOption.REPLACE_EXISTING);

            // Copy the edna source files
            //      ednaSource/src --> context/src
            //      ednaSource/setup.cfg --> context/setup.cfg
            //      ednaSource/setup.py --> context/setup.py

            File srcDir = new File(ednaSource.resolve("src").toUri());
            File destDir = new File(context.resolve("src").toUri());
            FileUtils.copyDirectory(srcDir, destDir);

            Files.copy(ednaSource.resolve("setup.cfg"), context.resolve("setup.cfg"), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(ednaSource.resolve("setup.py"), context.resolve("setup.py"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Get the image details
        String localImageName = ednaJob.getSpec().getApplicationname() + "-" +
                ednaJob.getSpec().getJobname() + ":" +
                ednaJob.getSpec().getJobimagetag();
        String remoteImageRepository = ednaJob.getSpec().getRegistryhost() + ":" +
                ednaJob.getSpec().getRegistryport() + "/" +
                ednaJob.getSpec().getApplicationname() + "-" +
                ednaJob.getSpec().getJobname();

        // Build the image
        File Dockerfile = new File(context.resolve("Dockerfile").toString());
        String imageId = dockerClient.buildImageCmd()
                .withBaseDirectory(new File(configuration.getEdnaAppdir()))
                .withDockerfile(Dockerfile)
                .withPull(true)
                .withNoCache(true)
                .withTags(Collections.singleton(localImageName))
                .exec(new BuildImageResultCallback())
                .awaitImageId();
        dockerClient.tagImageCmd(imageId, remoteImageRepository, ednaJob.getSpec().getJobimagetag()).exec();

        try {

            dockerClient.pushImageCmd(remoteImageRepository)
                        .withTag(ednaJob.getSpec().getJobimagetag())
                        .exec(new PushImageResultCallback())
                        .awaitCompletion();

        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
            // handle this somehow? if there is an error, shut down or DO NOT process this...
            //  have to think about this.
        }

        // Delete the source files, dockerignore, and Dockerfile
        //https://www.baeldung.com/java-delete-directory

        try {
            File srcDir = new File(context.resolve("src").toUri());
            FileUtils.deleteDirectory(srcDir);

            Files.delete(context.resolve("setup.cfg"));
            Files.delete(context.resolve("setup.py"));
            Files.delete(context.resolve("Dockerfile"));
            Files.delete(context.resolve(".dockerignore"));
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
