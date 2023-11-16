package basiclearner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Collection;
import java.util.Random;

import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.serialization.dot.GraphDOT;
import net.automatalib.visualization.dot.DOT;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;

import com.google.common.collect.Lists;

import de.learnlib.acex.analyzers.AcexAnalyzers;
import de.learnlib.algorithms.kv.mealy.KearnsVaziraniMealy;
import de.learnlib.algorithms.ttt.mealy.TTTLearnerMealy;
import de.learnlib.api.SUL;
import de.learnlib.api.algorithm.LearningAlgorithm;
import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.oracle.MembershipOracle.MealyMembershipOracle;
import de.learnlib.api.query.DefaultQuery;
import de.learnlib.filter.statistic.Counter;
import de.learnlib.filter.statistic.sul.ResetCounterSUL;
import de.learnlib.filter.statistic.sul.SymbolCounterSUL;
import de.learnlib.oracle.equivalence.MealyWMethodEQOracle;
import de.learnlib.oracle.equivalence.MealyWpMethodEQOracle;
import de.learnlib.oracle.equivalence.WMethodEQOracle;
import de.learnlib.oracle.equivalence.WpMethodEQOracle;
import de.learnlib.oracle.equivalence.mealy.RandomWalkEQOracle;
import de.learnlib.algorithms.lstar.ce.ObservationTableCEXHandlers;
import de.learnlib.algorithms.lstar.closing.ClosingStrategies;
import de.learnlib.algorithms.lstar.mealy.ExtensibleLStarMealy;
import de.learnlib.oracle.membership.SULOracle;
import de.learnlib.util.Experiment.MealyExperiment;
import net.automatalib.words.impl.GrowingMapAlphabet;

/**
 * General learning testing framework. All basic settings are at the top of this
 * file and can be configured by hard-coding or by simply changing them from
 * your own code. Method "runSimpleExperiment" learns a model and writes it to a
 * file. Method "runControlledExperiment" shows extra statistics and
 * intermediate hypotheses, which you can customize.
 * 
 * Based on the learner experiment setup of Joshua Moerman,
 * https://gitlab.science.ru.nl/moerman/Learnlib-Experiments
 * 
 * @author Ramon Janssen
 */
public class BasicLearner {
	// ***********************************************************************************//
	// Learning settings (hardcoded, simply set to a different value to change
	// learning) //
	// ***********************************************************************************//
	/**
	 * name to give to the resulting .dot-file and .pdf-file (extensions are added
	 * automatically)
	 */
	public static String // extension .pdf is added automatically
	FINAL_MODEL_FILENAME = "learnedModel", INTERMEDIATE_HYPOTHESIS_FILENAME = "hypothesis"; // a number gets appended
	// for every iteration
	/**
	 * For controlled experiments only: store every hypotheses as a file. Useful for
	 * 'debugging' if the learner does not terminate (hint: the TTT-algorithm
	 * produces many hypotheses).
	 */
	public static boolean saveAllHypotheses = true;
	/**
	 * For random walk, the chance to reset after every input
	 */
	public static double randomWalk_chanceOfResetting = 0.1;
	/**
	 * For random walk, the number of symbols that is tested in total (maybe with
	 * resets in between).
	 */
	public static int randomWalk_numberOfSymbols = 300;
	/**
	 * MaxDepth-parameter for W-method and Wp-method. This acts as the parameter 'n'
	 * for an n-complete test suite. Typically not larger than 3. Decrease for
	 * quicker runs.
	 */
	public static int w_wp_methods_maxDepth = 2;

	// ********************************************//
	// Predefined learning and testing algorithms //
	// ********************************************//
	/**
	 * The learning algorithms. LStar is the basic algorithm, TTT performs much
	 * faster but is a bit more inaccurate and produces more intermediate
	 * hypotheses, so test well.
	 */
	public enum LearningMethod {
		LStar, RivestSchapire, TTT, KearnsVazirani
	}

	/**
	 * The testing algorithms. Random walk is the simplest, but may perform badly on
	 * large models: the chance of hitting a hard-to-reach transition is very small.
	 * WMethod and WpMethod are smarter. With UserQueries, the user acts as
	 * equivalence oracle: have a look at the hypothesis, and try to think of one.
	 */
	public enum TestingMethod {
		RandomWalk, WMethod, WpMethod, UserQueries
	}

	public static LearningAlgorithm<MealyMachine<?, String, ?, String>, String, Word<String>> loadLearner(
			LearningMethod learningMethod, MealyMembershipOracle<String, String> sulOracle, Alphabet<String> alphabet) {
		switch (learningMethod) {
		case LStar:
			return new ExtensibleLStarMealy<String, String>(alphabet, sulOracle, Lists.<Word<String>>newArrayList(),
					ObservationTableCEXHandlers.CLASSIC_LSTAR, ClosingStrategies.CLOSE_SHORTEST);
		case RivestSchapire:
			return new ExtensibleLStarMealy<String, String>(alphabet, sulOracle, Lists.<Word<String>>newArrayList(),
					ObservationTableCEXHandlers.RIVEST_SCHAPIRE, ClosingStrategies.CLOSE_SHORTEST);
		case TTT:
			return new TTTLearnerMealy<String, String>(alphabet, sulOracle, AcexAnalyzers.LINEAR_FWD);
		case KearnsVazirani:
			return new KearnsVaziraniMealy<String, String>(alphabet, sulOracle, false, AcexAnalyzers.LINEAR_FWD);
		default:
			throw new RuntimeException("No learner selected");
		}
	}

	public static EquivalenceOracle<MealyMachine<?, String, ?, String>, String, Word<String>> loadTester(
			TestingMethod testMethod, SUL<String, String> sul, MealyMembershipOracle<String, String> sulOracle) {
		switch (testMethod) {
		// simplest method, but doesn't perform well in practice, especially for large
		// models
		case RandomWalk:
			return new RandomWalkEQOracle<String, String>(sul, randomWalk_chanceOfResetting, randomWalk_numberOfSymbols, true,
					new Random(123456l));
			// Other methods are somewhat smarter than random testing: state coverage,
			// trying to distinguish states, etc.
		case WMethod:
			return new MealyWMethodEQOracle<String, String>(sulOracle, w_wp_methods_maxDepth);
		case WpMethod:
			return new MealyWpMethodEQOracle<String, String>(sulOracle, w_wp_methods_maxDepth);
		case UserQueries:
			return new UserEQOracle(sul);
		default:
			throw new RuntimeException("No test oracle selected!");
		}
	}

	// **********************************************//
	// Methods to start the actual learning process //
	// **********************************************//
	/**
	 * Simple example of running a learning experiment
	 * 
	 * @param learner  Learning algorithm, wrapping the SUL
	 * @param eqOracle Testing algorithm, wrapping the SUL
	 * @param alphabet Input alphabet
	 * @throws IOException if the result cannot be written
	 */
	public static void runSimpleExperiment(
			LearningAlgorithm<MealyMachine<?, String, ?, String>, String, Word<String>> learner,
			EquivalenceOracle<MealyMachine<?, String, ?, String>, String, Word<String>> eqOracle,
			Alphabet<String> alphabet) throws IOException {
		MealyExperiment<String, String> experiment = new MealyExperiment<String, String>(learner, eqOracle, alphabet);
		experiment.run();
		System.out.println("Ran " + experiment.getRounds().getCount() + " rounds");
		produceOutput(FINAL_MODEL_FILENAME, experiment.getFinalHypothesis(), alphabet, true);
	}

	/**
	 * Simple example of running a learning experiment
	 * 
	 * @param sul            Direct access to SUL
	 * @param learningMethod One of the default learning methods from this class
	 * @param testingMethod  One of the default testing methods from this class
	 * @param alphabet       Input alphabet
	 * @throws IOException if the result cannot be written
	 */
	public static void runSimpleExperiment(SUL<String, String> sul, LearningMethod learningMethod,
			TestingMethod testingMethod, Collection<String> alphabet) throws IOException {
		Alphabet<String> learlibAlphabet = new GrowingMapAlphabet<String>(alphabet);
		LearningSetup learningSetup = new LearningSetup(sul, learningMethod, testingMethod, learlibAlphabet);
		runSimpleExperiment(learningSetup.learner, learningSetup.eqOracle, learlibAlphabet);
	}

	/**
	 * More detailed example of running a learning experiment. Starts learning, and
	 * then loops testing, and if counterexamples are found, refining again. Also
	 * prints some statistics about the experiment
	 * 
	 * @param learner   learner Learning algorithm, wrapping the SUL
	 * @param eqOracle  Testing algorithm, wrapping the SUL
	 * @param nrSymbols A counter for the number of symbols that have been sent to
	 *                  the SUL (for statistics)
	 * @param nrResets  A counter for the number of resets that have been sent to
	 *                  the SUL (for statistics)
	 * @param alphabet  Input alphabet
	 * @throws Exception
	 */
	public static void runControlledExperiment(
			LearningAlgorithm<MealyMachine<?, String, ?, String>, String, Word<String>> learner,
			EquivalenceOracle<MealyMachine<?, String, ?, String>, String, Word<String>> eqOracle, Counter nrSymbols,
			Counter nrResets, Alphabet<String> alphabet) throws Exception {
		try {
			// prepare some counters for printing statistics
			int stage = 0;
			long lastNrResetsValue = 0, lastNrSymbolsValue = 0;

			// learn the first hypothesis
			learner.startLearning();

			while (true) {
				// store hypothesis as file
				if (saveAllHypotheses) {
					String outputFilename = INTERMEDIATE_HYPOTHESIS_FILENAME + stage;
					produceOutput(outputFilename, learner.getHypothesisModel(), alphabet, false);
					System.out.println("model size " + learner.getHypothesisModel().getStates().size());
				}

				// Print statistics
				System.out.println(stage + ": " + Calendar.getInstance().getTime());
				// Log number of queries/symbols
				System.out.println("Hypothesis size: " + learner.getHypothesisModel().size() + " states");
				long roundResets = nrResets.getCount() - lastNrResetsValue,
						roundSymbols = nrSymbols.getCount() - lastNrSymbolsValue;
				System.out.println("learning queries/symbols: " + nrResets.getCount() + "/" + nrSymbols.getCount() + "("
						+ roundResets + "/" + roundSymbols + " this learning round)");
				lastNrResetsValue = nrResets.getCount();
				lastNrSymbolsValue = nrSymbols.getCount();

				// Search for CE
				DefaultQuery<String, Word<String>> ce = eqOracle.findCounterExample(learner.getHypothesisModel(),
						alphabet);

				// Log number of queries/symbols
				roundResets = nrResets.getCount() - lastNrResetsValue;
				roundSymbols = nrSymbols.getCount() - lastNrSymbolsValue;
				System.out.println("testing queries/symbols: " + nrResets.getCount() + "/" + nrSymbols.getCount() + "("
						+ roundResets + "/" + roundSymbols + " this testing round)");
				lastNrResetsValue = nrResets.getCount();
				lastNrSymbolsValue = nrSymbols.getCount();

				if (ce == null) {
					// No counterexample found, stop learning
					System.out.println("\nFinished learning!");
					produceOutput(FINAL_MODEL_FILENAME, learner.getHypothesisModel(), alphabet, true);
					break;
				} else {
					// Counterexample found, rinse and repeat
					System.out.println();
					stage++;
					learner.refineHypothesis(ce);
				}
			}
		} catch (Exception e) {
			String errorHypName = "hyp.before.crash.dot";
			produceOutput(errorHypName, learner.getHypothesisModel(), alphabet, true);
			throw e;
		}
	}

	/**
	 * More detailed example of running a learning experiment. Starts learning, and
	 * then loops testing, and if counterexamples are found, refining again. Also
	 * prints some statistics about the experiment
	 * 
	 * @param sul            Direct access to SUL
	 * @param learningMethod One of the default learning methods from this class
	 * @param testingMethod  One of the default testing methods from this class
	 * @param alphabet       Input alphabet
	 * @param alphabet       Input alphabet
	 * @throws IOException
	 */
	public static void runControlledExperiment(SUL<String, String> sul, LearningMethod learningMethod,
			TestingMethod testingMethod, Collection<String> alphabet) throws IOException {
		Alphabet<String> learnlibAlphabet = new GrowingMapAlphabet<String>(alphabet);
		LearningSetup learningSetup = new LearningSetup(sul, learningMethod, testingMethod, learnlibAlphabet);
		try {
			runControlledExperiment(learningSetup.learner, learningSetup.eqOracle, learningSetup.nrSymbols,
					learningSetup.nrResets, learnlibAlphabet);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("[ERROR]Cannot connect to the server, connection refused. Is it your FTP server online?:/");
		}
	}

	// ************************//
	// Some auxiliary methods //
	// ************************//
	/**
	 * Produces a dot-file and a PDF (if graphviz is installed)
	 * 
	 * @param fileName     filename without extension - will be used for the .dot
	 *                     and .pdf
	 * @param model
	 * @param alphabet
	 * @param verboseError whether to print an error explaing that you need graphviz
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void produceOutput(String fileName, MealyMachine<?, String, ?, String> model,
			Alphabet<String> alphabet, boolean verboseError) throws FileNotFoundException, IOException {
		PrintWriter dotWriter = new PrintWriter(fileName + ".dot");
		GraphDOT.write(model, alphabet, dotWriter);
		try {
			File file = new File(fileName + ".dot");
			DOT.runDOT(file, "pdf", new File(fileName + ".pdf"));
			System.out.println("result written to " + file.getAbsolutePath());
		} catch (Exception e) {
			if (verboseError) {
//				System.err.println("Warning: Install graphviz to convert dot-files to PDF");
//				System.err.println(e.getMessage());
			}
		}
		dotWriter.close();
	}

	/**
	 * Helper class to configure a learning and equivalence oracle. Tell it which
	 * learning and testing method you want, and it produces the corresponding
	 * oracles (and counters for statistics) as attributes.
	 */
	public static class LearningSetup {
		public final EquivalenceOracle<MealyMachine<?, String, ?, String>, String, Word<String>> eqOracle;
		public final LearningAlgorithm<MealyMachine<?, String, ?, String>, String, Word<String>> learner;
		public final Counter nrSymbols, nrResets;

		public LearningSetup(SUL<String, String> sul, LearningMethod learningMethod, TestingMethod testingMethod,
				Alphabet<String> alphabet) {
			// Wrap the SUL in a detector for non-determinism
			SUL<String, String> nonDetSul = new NonDeterminismCheckingSUL<String, String>(sul);
			// Wrap the SUL in counters for symbols/resets, so that we can record some
			// statistics
			SymbolCounterSUL<String, String> symbolCounterSul = new SymbolCounterSUL<String, String>("symbol counter",
					nonDetSul);
			ResetCounterSUL<String, String> resetCounterSul = new ResetCounterSUL<String, String>("reset counter",
					symbolCounterSul);
			nrSymbols = symbolCounterSul.getStatisticalData();
			nrResets = resetCounterSul.getStatisticalData();
			// we should use the sul only through those wrappers
			sul = resetCounterSul;
			// Most testing/learning-algorithms want a membership-oracle instead of a SUL
			// directly
			MealyMembershipOracle<String, String> sulOracle = new SULOracle<String, String>(sul);

			// Choosing an equivalence oracle
			eqOracle = loadTester(testingMethod, sul, sulOracle);

			// Choosing a learner
			learner = loadLearner(learningMethod, sulOracle, alphabet);
		}
	}
}
