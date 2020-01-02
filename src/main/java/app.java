import org.apache.spark.ml.classification.*;
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator;
import org.apache.spark.ml.feature.*;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class app {

    public static void main(String[] args) {
        SparkSession sparkSession = SparkSession.builder().appName("diabetes-mllib").master("local").getOrCreate();
        Dataset<Row> raw_data = sparkSession.read().format("csv")
                .option("header", "true")
                .option("inferSchema", "true")
                .load("/home/latif/Belgeler/internet_based_programming/project/DiabetesFinal/diabetes.csv");
        String[] headerList = {"Pregnancies", "Glucose", "BloodPressure", "SkinThickness", "Insulin", "BMI", "DiabetesPedigreeFunction", "Age", "Outcome"};

        List<String> headers = Arrays.asList(headerList);
        List<String> headersResult = new ArrayList<String>();
        for (String h : headers) {
            if (h.equals("Outcome")) {
                StringIndexer indexTmp = new StringIndexer().setInputCol(h).setOutputCol("label");
                raw_data = indexTmp.fit(raw_data).transform(raw_data);
                headersResult.add("label");
            } else {
                StringIndexer indexTmp = new StringIndexer().setInputCol(h).setOutputCol(h.toLowerCase() + "_cat");
                raw_data = indexTmp.fit(raw_data).transform(raw_data);
                headersResult.add(h.toLowerCase() + "_cat");
            }
        }

        String[] colList = headersResult.toArray(new String[headersResult.size()]);
        VectorAssembler vectorAssembler = new VectorAssembler().setInputCols(colList).setOutputCol("features");

        Dataset<Row> transform_data = vectorAssembler.transform(raw_data);
        Dataset<Row> final_data = transform_data.select("label", "features");
        Dataset<Row>[] datasets = final_data.randomSplit(new double[]{0.65, 0.35});
        Dataset<Row> train_data = datasets[0];
        Dataset<Row> test_data = datasets[1];


        NaiveBayes nb = new NaiveBayes();
        nb.setSmoothing(1);
        NaiveBayesModel model = nb.fit(train_data);





        Dataset<Row> predictions = model.transform(test_data);

        MulticlassClassificationEvaluator evaluator = new MulticlassClassificationEvaluator()
                .setLabelCol("label")
                .setPredictionCol("prediction")
                .setMetricName("accuracy");
        double evaluate = evaluator.evaluate(predictions);
        int test_data_number = (int)test_data.count();
        predictions.show(test_data_number);
        System.out.println("NaiveBayes Accuracy : " + evaluate);

    }
}