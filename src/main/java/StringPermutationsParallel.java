import org.bitcoinj.crypto.*;
import org.bitcoinj.wallet.DeterministicSeed;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicLong;

public class StringPermutationsParallel {
    public static AtomicLong count = new AtomicLong();
    public static long startTime = System.currentTimeMillis();

    public static void main(String[] args) {
        String[] strings = {
                "camera", "rhythm", "feature", "layer", "coconut", "ready",
                "need", "final", "north", "can", "early", "story",
                "stable", "report", "group", "depend", "employ", "problem",
                "monitor", "interest", "logic", "sausage", "toilet", "pencil"
        };
        int numThreads = 16;
        ForkJoinPool pool = new ForkJoinPool(numThreads);
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
                if((count.getAndIncrement() % 1000) == 0) {
                    long endTime = System.currentTimeMillis();
                    System.out.println(count.get() + ": " + (endTime - startTime) + "ms, Thread: " + Thread.currentThread().getName());
                    startTime = System.currentTimeMillis();
                }

                try {
                    // Generate the deterministic seed from the seed phrase
                    DeterministicSeed seed = new DeterministicSeed(Arrays.asList(arr), null, "", new SecureRandom().nextInt());
                    DeterministicKey masterKey = HDKeyDerivation.createMasterPrivateKey(seed.getSeedBytes());

                    DeterministicKey purpose44 = HDKeyDerivation.deriveChildKey(masterKey, new ChildNumber(44 | ChildNumber.HARDENED_BIT));
                    DeterministicKey purpose60 = HDKeyDerivation.deriveChildKey(purpose44, new ChildNumber(60 | ChildNumber.HARDENED_BIT));
                    DeterministicKey account0 = HDKeyDerivation.deriveChildKey(purpose60, new ChildNumber(0 | ChildNumber.HARDENED_BIT));
                    DeterministicKey externalChain = HDKeyDerivation.deriveChildKey(account0, ChildNumber.ZERO_HARDENED);
                    DeterministicKey childKey = HDKeyDerivation.deriveChildKey(externalChain, ChildNumber.ZERO);
                    
                    byte[] privateKeyBytes = childKey.getPrivKeyBytes();

                    BigInteger privateKey = Numeric.toBigInt(privateKeyBytes);

                    ECKeyPair keyPair = ECKeyPair.create(privateKey);
                    String publicKey = Numeric.toHexStringNoPrefix(keyPair.getPublicKey());

                    // Generate the Ethereum public address from the public key
                    String address = Keys.getAddress(publicKey);

                    // Print out the private key, public key, and address
//                    System.out.println("Private key: " + Numeric.toHexStringNoPrefix(privateKey));
//                    System.out.println("Public key: " + publicKey);
//                    System.out.println("Public address: " + address + ", Thread: " + Thread.currentThread().getName());

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
