package analyser;

import analyser.types.Type;

import java.util.ArrayList;
import java.util.Objects;

public record Function(String name, Type ret, ArrayList<Para> paras, String signature, String location) {
    public Function(String name, Type ret, String signature, String location) {
        this(name, ret, new ArrayList<>(), signature, location);
    }

    public void addPara(Para para) {
        paras.add(para);
    }

    @Override
    public String toString() {
        return "Function{" +
                "name='" + name + '\'' +
                ", ret=" + ret +
                ", paras=" + paras +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Function function)) return false;

        return Objects.equals(name, function.name) && Objects.equals(signature, function.signature);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(name);
        result = 31 * result + Objects.hashCode(signature);
        return result;
    }
}