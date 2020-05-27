package com.philips.swcoe.cerberus.cerebellum.codemetrics.java;

import com.philips.swcoe.cerberus.cerebellum.codemetrics.java.results.CodeMetricsClassResult;
import com.philips.swcoe.cerberus.cerebellum.codemetrics.java.results.CodeMetricsMethodResult;
import com.philips.swcoe.cerberus.cerebellum.codemetrics.java.results.CodeMetricsResult;
import io.vavr.control.Try;
import org.apache.commons.csv.CSVPrinter;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CodeMetricsHorizontalWriterService extends AbstractCodeMetricsWriterService  {
    private static Logger log = Logger.getLogger(CodeMetricsHorizontalWriterService.class);

    public CodeMetricsHorizontalWriterService(List<String> classConfig, List<String> methodConfig, char delimiter) throws IOException {
        super(classConfig, methodConfig, delimiter);
        csvPrinter = getCsvPrinterHorizontal(this.delimiter, reportWriter);
    }

    public String generateMetricsReport(List<CodeMetricsClassResult> codeMetricsClassResults) {
        codeMetricsClassResults.stream().forEach(getCodeMetricsClassResultConsumer());
        return this.getReport();
    }

    private Consumer<CodeMetricsClassResult> getCodeMetricsClassResultConsumer() {
        return codeMetricsClassResult -> {
            writeClassMetrics(csvPrinter, codeMetricsClassResult);
            writeMethodMetrics(csvPrinter, codeMetricsClassResult.getMethodMetrics());
        };
    }

    private void writeMethodMetrics(CSVPrinter csvPrinter, List<CodeMetricsMethodResult> codeMetricsMethodResults) {
        codeMetricsMethodResults.stream().forEach(codeMetricsMethodResult -> {
            List<String> dataToWriteForMethod = new ArrayList<String>();
            dataToWriteForMethod.add(codeMetricsMethodResult.getFile());
            dataToWriteForMethod.add(codeMetricsMethodResult.getMethodName());
            dataToWriteForMethod.add(codeMetricsMethodResult.getType());
            getStreamMetricsMethods(codeMetricsMethodResult.getClass().getMethods()).forEach(methodInMethodResult -> {
                Try.run(() -> processMetricResult(dataToWriteForMethod, (CodeMetricsResult) methodInMethodResult.invoke(codeMetricsMethodResult), methodConfig))
                        .onFailure(exception -> log.trace(exception.getStackTrace()));
            });
            writeReportData(csvPrinter, dataToWriteForMethod);
        });
    }

    private void writeClassMetrics(CSVPrinter csvPrinter, CodeMetricsClassResult codeMetricsClassResult) {
        List<String> dataToWriteForClass = new ArrayList<String>();
        dataToWriteForClass.add(codeMetricsClassResult.getFile());
        dataToWriteForClass.add(codeMetricsClassResult.getClassName());
        dataToWriteForClass.add(codeMetricsClassResult.getType());
        getStreamMetricsMethods(codeMetricsClassResult.getClass().getMethods()).forEach(methodInClassResult -> {
            Try.run(() -> processMetricResult(dataToWriteForClass, (CodeMetricsResult) methodInClassResult.invoke(codeMetricsClassResult), classConfig))
                    .onFailure(exception -> log.trace(exception.getStackTrace()));
        });
        writeReportData(csvPrinter, dataToWriteForClass);
    }

    private void processMetricResult(List<String> dataToWriteForClass, CodeMetricsResult codeMetricsResult, List<String> classConfig) {
        Try.of(() -> codeMetricsResult)
                .andThen(codeMetricsResultForClass -> Try.run(() -> pushMetricsToWrite(codeMetricsResultForClass, dataToWriteForClass, classConfig))
                        .onFailure(exception -> log.trace(exception.getStackTrace())));
    }


    private void writeReportData(CSVPrinter csvPrinter, List<String> dataToWrite) {
        if (dataToWrite.size() > 3) {
            Try.run(() -> csvPrinter.printRecord(dataToWrite.toArray()))
                    .onFailure(exception -> log.trace(exception.getStackTrace()));
        }
    }

    private void pushMetricsToWrite(CodeMetricsResult codeMetricsResult, List<String> dataToWrite, List<String> displayConfig) {
        List<String> metricsToDisplay = getMetricsToDisplayFromConfig(displayConfig);
        if(doesItMatterToDisplay(codeMetricsResult, metricsToDisplay)) {
            dataToWrite.add(codeMetricsResult.getMetricName());
            dataToWrite.add(String.valueOf(codeMetricsResult.getNewValue()));
            dataToWrite.add(String.valueOf(codeMetricsResult.getOldValue()));
        }
    }


}
