package com.daninichu.benchmark.grid;

import com.daninichu.benchmark.Main;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class HashSchemeBenchmark {
    public static void main(String[] args) throws Exception{
        Main.benchmark(HashSchemeBenchmark.class);
    }

    // -------------------------------------------------------------------------
    // Benchmark state
    // -------------------------------------------------------------------------

    @Param({"10000"})
    int n;

//    @Param({"31", "97", "1000000003"})
//    int a;


    private int[] x, y, values;

    @Setup
    public void setup() {
        values = new int[n];
        x = new int[n];
        y = new int[n];

        for (int i = 0; i < n; i++) {
            int x = (i % 200) - 100;
            int y = (i / 200) - 25;
            values[i] = i;

            this.x[i] = x;
            this.y[i] = y;
        }
    }

    @Benchmark
    public void hashCodeLinear(Blackhole bh) {
        HashingScheme scheme = new LinearHashingScheme(41);
        for (int i = 0; i < n; i++) bh.consume(scheme.hashCode(x[i], y[i]));
    }

//    @Benchmark
//    public void hashCodeFnv(Blackhole bh) {
//        HashingScheme scheme = new FnvHashingScheme();
//        for (int i = 0; i < n; i++) bh.consume(scheme.hashCode(x[i], y[i]));
//    }
//
//    @Benchmark
//    public void hashCodeSzudzik(Blackhole bh) {
//        HashingScheme scheme = new SzudzikHashingScheme();
//        for (int i = 0; i < n; i++) bh.consume(scheme.hashCode(x[i], y[i]));
//    }
//
//    @Benchmark
//    public void hashCodeCantor(Blackhole bh) {
//        HashingScheme scheme = new CantorHashingScheme();
//        for (int i = 0; i < n; i++) bh.consume(scheme.hashCode(x[i], y[i]));
//    }
}


abstract class HashingScheme{
    public abstract int hashCode(int x, int y);
    public abstract String toString();
}
class LinearHashingScheme extends HashingScheme{
    final int a;
    LinearHashingScheme(int a){
        this.a = a;
    }
    public int hashCode(int x, int y){
        return 65537* x + y;
    }
    public String toString(){
        return a+" * x + y";
    }
}