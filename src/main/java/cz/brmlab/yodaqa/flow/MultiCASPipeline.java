package cz.brmlab.yodaqa.flow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.CasIterator;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;

import cz.brmlab.yodaqa.flow.asb.ParallelEngineFactory;
import cz.brmlab.yodaqa.io.interactive.InteractiveAnswerPrinter;
import cz.brmlab.yodaqa.io.interactive.InteractiveQuestionReader;
import cz.brmlab.yodaqa.pipeline.YodaQA;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;



import org.apache.uima.UimaContext;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.CasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import cz.brmlab.yodaqa.flow.dashboard.Question;
import cz.brmlab.yodaqa.flow.dashboard.QuestionDashboard;
import cz.brmlab.yodaqa.model.Question.QuestionInfo;






import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;







// Interface definition
import QPMThrift.QPM;

/**
 * By "Philipp W".
 * 
 * https://groups.google.com/forum/#!topic/uimafit-users/yA0w2Q8tGNE
 *
 * "I played with this issue a bit more today and ended up writing an
 * implementation that runs a pipeline of analysis engines (similar to
 * SimplePipeline), but that supports things like CASMultipliers. I'm sure it
 * doesn't completely support all the variations of analysis engines that UIMA
 * theoretically offers (I've found the UIMA documentation badly lacking in
 * descriptions of the contracts that go with its interfaces), and I haven't
 * tested it beyond my straightforward use case, so I offer it as-is. Since I
 * don't have a developer's account for uimaFIT (and since I don't want to mess
 * with other people's code in SimplePipeline).
 *
 * I won't try to integrate this into the project, but if someone would like to
 * use all or part of it for uimaFIT please feel free to do so without
 * restrictions."
 *
 * Customized to a degree, e.g. fixed to work with Aggregate AEs.
 *
 * XXX: This code could probably do with quite a few cleanups. */

public final class MultiCASPipeline implements QPM.Iface {

	private static List<ResourceMetaData> _analysisComponentsMetadata;
	private static List<AnalysisEngine> _analysisEngines;
	private static int index = -1;

	public MultiCASPipeline() throws Exception {
		CollectionReaderDescription reader = createReaderDescription(
				InteractiveQuestionReader.class,
				InteractiveQuestionReader.PARAM_LANGUAGE, "en");

		AnalysisEngineDescription pipeline = YodaQA.createEngineDescription();

		AnalysisEngineDescription printer = createEngineDescription(
				InteractiveAnswerPrinter.class);

		ParallelEngineFactory.registerFactory(); // comment out for a linear single-thread flow
		/* XXX: Later, we will want to create an actual flow
		 * to support scaleout. */
		runPipeline(reader,
				pipeline,
				printer);
	}

	public String handleQuery(String query) {
		/*
		 * Run the pipeline
		 */
		++index;



		Question q = new Question(Integer.toString(index), query);
		QuestionDashboard.getInstance().askQuestion(q);
		QuestionDashboard.getInstance().getQuestionToAnswer();




		try {
			CAS cas = null;

			cas = CasCreationUtils.createCas(_analysisComponentsMetadata);

			JCas jcas = cas.getJCas();
			jcas.setDocumentLanguage("en");

			QuestionInfo qInfo = new QuestionInfo(jcas);
			qInfo.setSource("interactive");
			qInfo.setQuestionId(Integer.toString(index));
			qInfo.addToIndexes(jcas);
			jcas.setDocumentText(query);







			runAnalysisEngines(_analysisEngines, 0, cas);
		} catch (Exception e) {
		}



		String answer = "";

		// The name of the file to open.
		String fileName = "/Users/yba/Documents/U/Sirius/yodaqa-master/answer.txt";

		// This will reference one line at a time
		String line = null;

		try {
			// FileReader reads text files in the default encoding.
			FileReader fileReader = 
			    new FileReader(fileName);

			// Always wrap FileReader in BufferedReader.
			BufferedReader bufferedReader = 
			    new BufferedReader(fileReader);

			while((line = bufferedReader.readLine()) != null) {
			    answer += line;
			}   

			// Always close files.
			bufferedReader.close();  
		} catch (Exception e) {

		}
       




		return answer;

	}

    public void ping() {
    	System.out.println("pinged");
    }





	public static void runPipeline(
			CollectionReaderDescription collectionReaderDescription,
			AnalysisEngineDescription ... analysisEngineDescriptions
			) throws UIMAException, IOException {

		/*
		 * Create a merged type system for all components in the pipeline
		 */
		List<TypeSystemDescription> typeSystemDescriptions = new ArrayList<TypeSystemDescription>();
		typeSystemDescriptions.add(collectionReaderDescription.getCollectionReaderMetaData().getTypeSystem());
		for( AnalysisEngineDescription analysisEngineDescription : analysisEngineDescriptions ) {
			typeSystemDescriptions.add(analysisEngineDescription.getAnalysisEngineMetaData().getTypeSystem());
		}

		TypeSystemDescription mergedTypeSystemDescription = CasCreationUtils.mergeTypeSystems(typeSystemDescriptions);


		/*
		 * Collect the metadata of all components (in order to create a CAS using CasCreationUtils),
		 * set each component's type system to the shared merged type system, and instantiate a
		 * CollectionReader / AnalysisEngine for each component.
		 */
		List<ResourceMetaData> analysisComponentsMetadata = new ArrayList<ResourceMetaData>();

		collectionReaderDescription.getCollectionReaderMetaData().setTypeSystem(mergedTypeSystemDescription);
		analysisComponentsMetadata.add(collectionReaderDescription.getMetaData());
		CollectionReader collectionReader = CollectionReaderFactory.createCollectionReader(collectionReaderDescription);

		List<AnalysisEngine> analysisEngines = new ArrayList<AnalysisEngine>();
		for( AnalysisEngineDescription analysisEngineDescription : analysisEngineDescriptions ) {
			if (analysisEngineDescription.isPrimitive())
				analysisEngineDescription.getAnalysisEngineMetaData().setTypeSystem(mergedTypeSystemDescription);
			analysisComponentsMetadata.add(analysisEngineDescription.getMetaData());

			AnalysisEngine analysisEngine = UIMAFramework.produceAnalysisEngine(analysisEngineDescription);
			analysisEngines.add(analysisEngine);
		}


		_analysisComponentsMetadata = analysisComponentsMetadata; // intiialize the static member variable
		_analysisEngines = analysisEngines;

		// /*
		//  * Run the pipeline
		//  */
		// CAS cas = null;
		// while( collectionReader.hasNext() ) {
		// 	if( cas == null ) // create a new CAS
		// 		cas = CasCreationUtils.createCas(analysisComponentsMetadata);
		// 	else // reuse the existing CAS to save resources
		// 		cas.reset();

		// 	collectionReader.getNext(cas);
		// 	runAnalysisEngines(analysisEngines, 0, cas);
		// }
		// if( cas != null ) {
		// 	cas.release();
		// }

		// /*
		//  * Clean up: not completely sure if all of these need to be called, documentation on the calling
		//  * protocols for UIMA interfaces is pretty spotty.
		//  */
		// collectionReader.close();
		// collectionReader.destroy();

		// for( AnalysisEngine analysisEngine : analysisEngines ) {
		// 	analysisEngine.batchProcessComplete();
		// 	analysisEngine.collectionProcessComplete();
		// 	analysisEngine.destroy();
		// }
	}

	private static void runAnalysisEngines(List<AnalysisEngine> analysisEngines, int index, CAS cas)
			throws AnalysisEngineProcessException {
		if( index >= analysisEngines.size() ) {
			// base case, this CAS has been run through to the end of the pipeline
			return;
		}

		// recursive case: run one AE, then do recursive call(s) on the rest

		AnalysisEngine analysisEngine = analysisEngines.get(index);
		if( ! analysisEngine.getAnalysisEngineMetaData().isSofaAware() ) {
			// I assume this is what should be done in this case. I don't want to mess with
			// SOFA mappings -- can use an aggregate analysis engine if that's necessary.
			cas = cas.getView(CAS.NAME_DEFAULT_SOFA);
		}

		if( analysisEngine.getAnalysisEngineMetaData().getOperationalProperties().getOutputsNewCASes() ) {
			// This could be a CasMultiplier, but the UIMA interface doesn't specify. All we
			// know is, this AE may output any number of CASes (including 0), and we need
			// to use a different "process" interface
			CasIterator casIterator = analysisEngine.processAndOutputNewCASes(cas);
			while( casIterator.hasNext() ) {
				// Do one recursive call on the rest of the pipeline for each CAS
				// that the analysis engine produces.
				CAS newCAS = casIterator.next();
				runAnalysisEngines(analysisEngines, index+1, newCAS);
				if( newCAS != cas ) {
					// If this is a new CAS produced by this analysis engine, we consider
					// ourselves responsible for cleaning it up. But if the AE just passed
					// on the CAS we gave it, we leave the clean up to our caller.
					newCAS.release();
				}
			}
		} else {
			// Run the CAS through this AE, then run the rest of the pipeline
			// recursively.
			analysisEngine.process(cas);
			runAnalysisEngines(analysisEngines, index+1, cas);
		}
	}
}
