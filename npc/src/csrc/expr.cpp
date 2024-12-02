#include <expr.hpp>
#include <numeric>
#include <string>
#include <vector>
#include <regex>

std::vector<Expr::Token> Expr::get_tokens(std::string expr){
    std::vector<Token> tokens;

    std::regex token_regex(R"(\s+|(0[xX][0-9a-fA-F]+)|([0-9]+)|(==)|(.))");
    
    std::sregex_iterator it(expr.begin(), expr.end(), token_regex);
    std::sregex_iterator end;

    while(it != end){
        std::smatch match = *it;
        if(match[1].matched){
        }
        else if(match[2].matched){
            tokens.push_back(Token(HEX, match.str()));
        }
        else if(match[3].matched){
            tokens.push_back(Token(DECIMAL, match.str()));
        }
        else if(match[4].matched){
            tokens.push_back(Token(EQ, match.str()));
        }
        it++;
    }

    return tokens;
}

std::string Expr::eval(std::string expr){
    std::vector<Token> tokens = get_tokens(expr);

    return std::accumulate(tokens.begin(), tokens.end(), std::string(), [](std::string acc, Token token){
        return acc + std::to_string(token.get_val()) + '\n';
    });
}
