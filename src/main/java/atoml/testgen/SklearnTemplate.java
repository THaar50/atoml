package atoml.testgen;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import atoml.classifiers.Algorithm;
import atoml.metamorphic.MetamorphicTest;
import atoml.smoke.SmokeTest;

public class SklearnTemplate implements TemplateEngine {

	/**
	 * Algorithm that is tested
	 */
	final Algorithm algorithmUnderTest;
	
	/**
	 * Constructor. Creates a new WekaTemplate. 
	 * @param algorithmUnderTest
	 */
	public SklearnTemplate(Algorithm algorithmUnderTest) {
		this.algorithmUnderTest = algorithmUnderTest;
	}
	
	@Override
	public String getClassName() {
		return "test_" + algorithmUnderTest.getName();
	}

	@Override
	public String getFilePath() {
		return getClassName() + ".py";
	}
	
	@Override
	public Map<String, String> getClassReplacements() {
		String importStatement = "from " + algorithmUnderTest.getPackage() + " import " + algorithmUnderTest.getClassName();;
		
		StringBuilder parameterString = new StringBuilder();
		parameterString.append("    params = [");
		boolean isFirst = true;
		if( algorithmUnderTest.getParameterCombinations().size()>0 ) {
			for( Map<String,String> parameterCombination :  algorithmUnderTest.getParameterCombinations()) {
				String curParamString = getParameterString(parameterCombination);
				if( !isFirst ) {
					parameterString.append("              ");
				}
				parameterString.append("(\"" + curParamString + "\", " + curParamString +  "),\n");
				isFirst = false;
			}
			parameterString.append("             ]");
		} else {
			// TODO This does not work properly yet, because it would generate an empty kwargs dict
			parameterString = parameterString.append("            \"{}\", {}]");
		}
		
		Map<String, String> replacements = new HashMap<>();
		replacements.put("<<<IMPORTCLASSIFIER>>>", importStatement);
		replacements.put("<<<HYPERPARAMETERS>>>", parameterString.toString());
		return replacements;
	}

	@Override
	public Map<String, String> getSmoketestReplacements(SmokeTest smokeTest) {
		Map<String, String> replacements = new HashMap<>();
		replacements.put("<<<CLASSIFIER>>>", algorithmUnderTest.getClassName()+"(**kwargs)");
		return replacements;
	}

	@Override
	public Map<String, String> getMorphtestReplacements(MetamorphicTest metamorphicTest) {
		String morphTestdata;
		String morphClassIndex;
		switch(metamorphicTest.getPredictionType()) {
		case ORDERED_DATA:
			morphTestdata = "data_morph_df";
			morphClassIndex = "class_index_morph";
			break;
		case SAME_CLASSIFIER:
			morphTestdata = "data_original_df";
			morphClassIndex = "class_index_original";
			break;
		default:
			throw new RuntimeException("could not generate tests, unknown morph test class");
		}
		
		String morphRelation;
		switch(metamorphicTest.getPredictionRelation()) {
		case EQUAL:
			morphRelation = "prediction_expected = prediction_original;\n";
			morphRelation += "            scores_expected = scores_original[:,0]";
			break;
		case INVERTED:
			morphRelation = "prediction_expected = abs(1-prediction_original)\n";
			morphRelation += "            scores_expected = scores_original[:,1]";
			break;
		default:
			throw new RuntimeException("could not generate tests, unknown morph prediction relation type");
		}
		
		Map<String, String> replacements = new HashMap<>();
		replacements.put("<<<CLASSIFIER>>>", algorithmUnderTest.getClassName()+"(**kwargs)");
		replacements.put("<<<MORPHTESTDATA>>>", morphTestdata);
		replacements.put("<<<MORPHCLASSINDEX>>>", morphClassIndex);
		replacements.put("<<<EXPECTEDMORPHEDCLASS>>>", morphRelation);
		return replacements;
	}
	
	/**
	 * creates a string for the test of a parameter combination
	 * @return parameters string
	 */
	private static String getParameterString(Map<String, String> parameterCombination) {
		StringBuilder parameters = new StringBuilder();
		parameters.append("{");
		for( Entry<String, String> parameter : parameterCombination.entrySet()) {
			parameters.append("'"+parameter.getKey()+"':");
			// TODO need to use parameter type here to decide whether there are quotes
			parameters.append(""+parameter.getValue()+",");
		}
		parameters.append("}");
		return parameters.toString();
	}
}
