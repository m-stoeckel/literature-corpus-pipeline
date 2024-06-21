package org.texttechnologylab.slc;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import org.apache.uima.jcas.cas.Sofa;
import org.apache.uima.jcas.tcas.DocumentAnnotation;
import org.dkpro.core.api.resources.CompressionMethod;
import org.texttechnologylab.DockerUnifiedUIMAInterface.DUUIComposer;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIDockerDriver;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIRemoteDriver;
import org.texttechnologylab.DockerUnifiedUIMAInterface.driver.DUUIUIMADriver;
import org.texttechnologylab.DockerUnifiedUIMAInterface.io.DUUIAsynchronousProcessor;
import org.texttechnologylab.DockerUnifiedUIMAInterface.io.DUUICollectionReader;
import org.texttechnologylab.DockerUnifiedUIMAInterface.io.reader.DUUIFileReader;
import org.texttechnologylab.DockerUnifiedUIMAInterface.lua.DUUILuaContext;
import org.texttechnologylab.slc.components.AnnotationDropper;
import org.texttechnologylab.slc.io.UriInfixXmiWriter;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

public class Main {


    static class Image {
        public String name;

        public Image(String name) {
            this.name = name;
        }

        public String resolve(String version) {
            return String.join(
                    ":",
                    this.name,
                    version
            );
        }

        public String resolve() {
            return this.resolve("latest");
        }
    }

    static class SlcImage extends Image {
        public String cuda;

        public SlcImage(
                String name,
                String cuda
        ) {
            super("docker.texttechnologylab.org/duui-slc-" + name);
            this.cuda = cuda;
        }

        public String resolve(String version) {
            return this.name + "/" + this.cuda + ":" + version;
        }

        public static String spacy(String version) {
            return new SlcImage(
                    "spacy",
                    "cu124"
            ).resolve(version);
        }

        public static String stanza(String version) {
            return new SlcImage(
                    "stanza",
                    "cu121"
            ).resolve(version);
        }
    }

    public static void main(String[] args) throws Exception {
        DUUICollectionReader readerEn = new DUUIFileReader(
                "/storage/xmi/Gutenberg/html/v0.5.1/en/",
                ".xmi.bz2"
        );
        DUUICollectionReader readerDe = new DUUIFileReader(
                "/storage/xmi/Gutenberg/html/v0.5.1/de/",
                ".xmi.bz2"
        );
        DUUIAsynchronousProcessor processor = new DUUIAsynchronousProcessor(
                readerEn,
                readerDe
        );

        DUUIComposer composer = new DUUIComposer()
                .withSkipVerification(true)
                .withWorkers(4)
                .withCasPoolsize(6)
                .withLuaContext(new DUUILuaContext().withJsonLibrary());

        composer.addDriver(new DUUIUIMADriver());
        composer.addDriver(new DUUIDockerDriver());
        composer.addDriver(new DUUIRemoteDriver());

//        composer.add(new DUUIRemoteDriver.Component("http://localhost:10001").build());
        composer.add(new DUUIDockerDriver.Component(SlcImage.spacy("0.3.1"))
                             .withGPU(true)
                             .build());
        composer.add(new DUUIUIMADriver.Component(createEngineDescription(
                UriInfixXmiWriter.class,
                UriInfixXmiWriter.PARAM_TARGET_LOCATION,
                "/storage/xmi/Gutenberg/annotated/0.5.1/",
                UriInfixXmiWriter.PARAM_COMPRESSION,
                CompressionMethod.BZIP2,
                UriInfixXmiWriter.PARAM_OVERWRITE,
                true,
                UriInfixXmiWriter.PARAM_URI_INFIX,
                "spacy"
        )));

        composer.add(new DUUIUIMADriver.Component(createEngineDescription(
                AnnotationDropper.class,
                AnnotationDropper.PARAM_TYPES_TO_RETAIN,
                new String[]{Sofa._TypeName, DocumentAnnotation._TypeName, DocumentMetaData._TypeName, Sentence._TypeName, Paragraph._TypeName}
        )));
        composer.add(new DUUIUIMADriver.Component(createEngineDescription(
                UriInfixXmiWriter.class,
                UriInfixXmiWriter.PARAM_TARGET_LOCATION,
                "/storage/xmi/Gutenberg/annotated/0.5.1/",
                UriInfixXmiWriter.PARAM_COMPRESSION,
                CompressionMethod.BZIP2,
                UriInfixXmiWriter.PARAM_OVERWRITE,
                true,
                UriInfixXmiWriter.PARAM_URI_INFIX,
                "plain"
        )));

////        composer.add(new DUUIRemoteDriver.Component("http://localhost:10002").build());
//        composer.add(new DUUIDockerDriver.Component(SlcImage.stanza("0.4.0"))
//                             .withGPU(true)
//                             .build());
//        composer.add(new DUUIUIMADriver.Component(createEngineDescription(
//                UriInfixXmiWriter.class,
//                UriInfixXmiWriter.PARAM_OVERWRITE,
//                true,
//                UriInfixXmiWriter.PARAM_TARGET_LOCATION,
//                "/hot_storage/Data/Gutenberg/xmi/annotated/",
//                UriInfixXmiWriter.PARAM_URI_INFIX,
//                "stanza"
//        )));

        composer.run(
                processor,
                "slc-pipeline"
        );
        composer.shutdown();
    }
}