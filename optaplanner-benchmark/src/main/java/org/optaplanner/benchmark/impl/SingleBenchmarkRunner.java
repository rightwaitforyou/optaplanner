/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.benchmark.impl;

import java.util.concurrent.Callable;

import org.optaplanner.benchmark.impl.statistic.SingleStatistic;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.impl.domain.solution.descriptor.SolutionDescriptor;
import org.optaplanner.core.impl.solution.Solution;
import org.optaplanner.core.impl.solver.DefaultSolver;
import org.optaplanner.core.impl.solver.scope.DefaultSolverScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingleBenchmarkRunner implements Callable<SingleBenchmarkRunner> {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    private final SingleBenchmarkResult singleBenchmarkResult;

    private Throwable failureThrowable = null;

    public SingleBenchmarkRunner(SingleBenchmarkResult singleBenchmarkResult) {
        this.singleBenchmarkResult = singleBenchmarkResult;
    }

    public SingleBenchmarkResult getSingleBenchmarkResult() {
        return singleBenchmarkResult;
    }

    public Throwable getFailureThrowable() {
        return failureThrowable;
    }

    public void setFailureThrowable(Throwable failureThrowable) {
        this.failureThrowable = failureThrowable;
    }

    // ************************************************************************
    // Benchmark methods
    // ************************************************************************

    public SingleBenchmarkRunner call() {
        Runtime runtime = Runtime.getRuntime();
        ProblemBenchmark problemBenchmark = singleBenchmarkResult.getProblemBenchmark();
        Solution inputSolution = problemBenchmark.readPlanningProblem();
        if (!problemBenchmark.getPlannerBenchmark().hasMultipleParallelBenchmarks()) {
            runtime.gc();
            singleBenchmarkResult.setUsedMemoryAfterInputSolution(runtime.totalMemory() - runtime.freeMemory());
        }
        logger.trace("Benchmark inputSolution has been read for singleBenchmarkResult ({}).",
                singleBenchmarkResult.getName());

        // Intentionally create a fresh solver for every SingleBenchmarkResult to reset Random, tabu lists, ...
        Solver solver = singleBenchmarkResult.getSolverBenchmark().getSolverConfig().buildSolver();

        for (SingleStatistic singleStatistic : singleBenchmarkResult.getSingleStatisticMap().values()) {
            singleStatistic.open(solver);
        }

        solver.setPlanningProblem(inputSolution);
        solver.solve();
        long timeMillisSpend = solver.getTimeMillisSpend();
        Solution outputSolution = solver.getBestSolution();

        SolutionDescriptor solutionDescriptor = ((DefaultSolver) solver).getSolutionDescriptor();
        singleBenchmarkResult.setPlanningEntityCount(solutionDescriptor.getEntityCount(outputSolution));
        problemBenchmark.registerProblemScale(solutionDescriptor.getProblemScale(outputSolution));
        singleBenchmarkResult.setScore(outputSolution.getScore());
        singleBenchmarkResult.setTimeMillisSpend(timeMillisSpend);
        DefaultSolverScope solverScope = ((DefaultSolver) solver).getSolverScope();
        singleBenchmarkResult.setCalculateCount(solverScope.getCalculateCount());

        for (SingleStatistic singleStatistic : singleBenchmarkResult.getSingleStatisticMap().values()) {
            singleStatistic.close(solver);
            singleStatistic.writeCsvStatisticFile();
        }
        problemBenchmark.writeOutputSolution(singleBenchmarkResult, outputSolution);
        return this;
    }

    public String getName() {
        return singleBenchmarkResult.getName();
    }

    @Override
    public String toString() {
        return singleBenchmarkResult.toString();
    }

}