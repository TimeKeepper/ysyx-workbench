#include <expr.hpp>
#include <numeric>
#include <string>
#include <vector>

std::vector<Expr::Token> Expr::get_tokens(std::string expr){
    std::vector<Token> tokens;

    auto words_begin = std::sregex_iterator(expr.begin(), expr.end(), this->token_regex);
    auto words_end = std::sregex_iterator();

    for (std::sregex_iterator i = words_begin; i != words_end; ++i) {
        std::smatch match = *i;
        for (size_t j = 1; j < match.size(); ++j) {
            if (match[j].str().size() > 0) {
                Token_Type type = static_cast<Token_Type>(j - 1);
                tokens.push_back(Token(type, match[j].str()));
            }
        }
    }

    return tokens;
}

std::string Expr::eval(std::string expr){
    std::vector<Token> tokens = get_tokens(expr);

    return std::accumulate(tokens.begin(), tokens.end(), std::string(), [](std::string acc, Token token){
        return acc + std::to_string(token.get_val()) + '\n';
    });
}
