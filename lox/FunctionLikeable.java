package lox;

import java.util.List;

public interface FunctionLikeable {
    List<Token> getParams();
    List<Stmt> getBody();
}
