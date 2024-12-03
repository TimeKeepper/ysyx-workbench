#include <expr.hpp>
#include <numeric>
#include <string>
#include <vector>
#include <regex>

std::vector<Expr::Token> Expr::get_tokens(std::string expr){
    std::vector<Token> tokens;
    std::string current_expr = expr;

    while(current_expr.size() > 0){
        for(auto token_pattern : token_patterns){
            std::smatch match;
            if(std::regex_search(current_expr, match, token_pattern.second, std::regex_constants::match_continuous)) continue;
            
            if(token_pattern.first != SPACE) tokens.push_back(Token(token_pattern.first, match.str()));
            current_expr = match.suffix();
            break;
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
