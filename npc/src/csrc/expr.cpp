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
            if(!std::regex_search(current_expr, match, token_pattern.second, std::regex_constants::match_continuous)) continue;

            if(token_pattern.first != TokenType::SPACE) tokens.push_back(Token(token_pattern.first, match.str()));
            current_expr = match.suffix();
            break;
        }
    }

    return tokens;
}

std::vector<Expr::Token> Expr::RPN(std::vector<Token> tokens){
    std::vector<Token> output;
    std::vector<Token> stack;

    for(auto token : tokens){
        if(token.type == TokenType::DECIMAL || token.type == TokenType::HEX){
            output.push_back(token);
        }
        else if(token.type == TokenType::EQ){
            stack.push_back(token);
        }
    }

    output.insert(output.end(), stack.begin(), stack.end());
    return output;
}

std::string Expr::eval(std::string expr){
    std::vector<Token> tokens = RPN(get_tokens(expr));

    std::vector<uint32_t> stack;
    for(auto token : tokens){
        if(token.type == TokenType::DECIMAL || token.type == TokenType::HEX){
            stack.push_back(token.get_val());
        }
        else if(token.type == TokenType::EQ){
            uint32_t a = stack.back();
            stack.pop_back();
            uint32_t b = stack.back();
            stack.pop_back();
            stack.push_back(a == b);
        }
    }

    // return std::accumulate(tokens.begin(), tokens.end(), std::string(), [](std::string acc, Token token){ return acc + token.value + '\n'; });

    return std::to_string(stack.back());
}
