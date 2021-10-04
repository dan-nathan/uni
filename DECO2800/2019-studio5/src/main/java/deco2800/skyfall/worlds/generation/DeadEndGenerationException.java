package deco2800.skyfall.worlds.generation;

public class DeadEndGenerationException extends Exception {
    public DeadEndGenerationException(String s){
        super(s);
    }
    DeadEndGenerationException(String s, Exception e){
        super(s,e);
    }
}
