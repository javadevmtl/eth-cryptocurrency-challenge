import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;
import org.web3j.crypto.Keys;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicLong;

public class StringPermutationsParallel {
    static AtomicLong count = new AtomicLong();
    static long startTime = System.currentTimeMillis();
    static ForkJoinPool pool;

    public static void main(String[] args) {
        String[] strings = {
                "camera", "rhythm", "feature", "layer", "coconut", "ready",
                "need", "final", "north", "can", "early", "story",
                "stable", "report", "group", "depend", "employ", "problem",
                "monitor", "interest", "logic", "sausage", "toilet", "pencil"
        };
        int numThreads = 16;
        pool = new ForkJoinPool(numThreads);
        pool.invoke(new PermuteAction(strings, 0, numThreads));


    }

    private static class PermuteAction extends RecursiveAction {
        private final String[] arr;
        private final int index;
        private final int numThreads;

        private PermuteAction(String[] arr, int index, int numThreads) {
            this.arr = arr;
            this.index = index;
            this.numThreads = numThreads;
        }

        @Override
        protected void compute() {
            if (index == arr.length - 1) {
                if((count.incrementAndGet() % 1000) == 0) {
                    long endTime = System.currentTimeMillis();
                    System.out.println(count.get() + ": " + (endTime - startTime) + "ms, Thread: " + Thread.currentThread().getName());
                    startTime = System.currentTimeMillis();
                }

                try {
                    long creationTimeSeconds = System.currentTimeMillis() / 1000L;
                    DeterministicSeed deterministicSeed = new DeterministicSeed(Arrays.asList(arr), null, "", creationTimeSeconds);
                    DeterministicKeyChain keyChain = DeterministicKeyChain.builder().seed(deterministicSeed).build();

                    int index = 0; // index of the key pair
//                    String derivationPath = "m/44'/60'/0'/0/" + index; // Ethereum derivation path
                    List<ChildNumber> path = new ArrayList<>();
                    path.add(new ChildNumber(44, true)); // BIP44 purpose
                    path.add(new ChildNumber(60, true)); // ETH coin type
                    path.add(new ChildNumber(0, true)); // account number
                    path.add(new ChildNumber(0, false)); // external chain (i.e. public keys)
                    path.add(new ChildNumber(0, false)); // index of the child key

                    DeterministicKey key = keyChain.getKeyByPath(path, true);
                    String address = "0x" + Keys.getAddress(key.getPublicKeyAsHex());

                    if("0xb6f420204511C7fE9Dd3DE14266a260e8f11aC37".equals(address)) {
                        System.out.println("We broke it: " + String.join(" ", arr));

                        pool.shutdown();
                        System.exit(0);
                    }

                }catch(Exception ex) {
                    ex.printStackTrace();
                }

                return;
            }
            int numActions = Math.min(numThreads, arr.length - index);
            PermuteAction[] actions = new PermuteAction[numActions];
            for (int i = 0; i < numActions; i++) {
                swap(arr, index, index + i);
                actions[i] = new PermuteAction(Arrays.copyOf(arr, arr.length), index + 1, numThreads);
            }
            invokeAll(actions);
            for (int i = 0; i < numActions; i++) {
                swap(arr, index, index + i);
            }
        }

        private static void swap(String[] arr, int i, int j) {
            String temp = arr[i];
            arr[i] = arr[j];
            arr[j] = temp;
        }
    }
}
