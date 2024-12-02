#include <expr.hpp>
#include <numeric>
#include <string>
#include <vector>
#include <regex>

std::vector<Expr::Token> Expr::get_tokens(std::string expr){
    std::vector<Token> tokens;
    
    while(expr.size() > 0){
        for(auto pattern : token_patterns){
            std::smatch match;
            if(std::regex_search(expr, match, pattern.second)){
                std::cout << match.str() << std::endl;
                std::cout << pattern.first << std::endl;
                tokens.push_back(Token(pattern.first, match.str()));
                expr = match.suffix();
                break;
            }
        }
    }

    return tokens;
}

std::string Expr::eval(std::string expr){
    std::vector<Token> tokens = get_tokens(expr);

    return std::accumulate(tokens.begin(), tokens.end(), std::string(), [](std::string acc, Token token){
        return acc + token.value + '\n';
    });
}
