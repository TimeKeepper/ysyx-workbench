#include <expr.hpp>
#include <vector>
#include <regex>

typedef enum {
    SPACE,
    DECIMAL,
    EQ,
} Token_Type;

// Token map with index
std::map<Token_Type, uint32_t> token_map = {
    {SPACE, 0},
    {DECIMAL, 1},
    {EQ, 2},
};

struct Token {
    Token_Type type;
    std::string value;

    Token(Token_Type type, const std::string& value) : type(type), value(value) {};
};

std::string Expr::eval(std::string expr){
    std::vector<Token> tokens;
    
    std::regex token_regex(
        R"(^(\s+)|([0-9]+)|(==))"
    );

    auto words_begin = std::sregex_iterator(expr.begin(), expr.end(), token_regex);
    auto words_end = std::sregex_iterator();

    for (std::sregex_iterator i = words_begin; i != words_end; ++i) {
        std::smatch match = *i;
        for (size_t j = 1; j < match.size(); ++j) {
            if (match[j].matched) {
                switch (j) {
                    case 1:
                        tokens.push_back(Token(SPACE, match[j].str()));
                        break;
                    case 2:
                        tokens.push_back(Token(DECIMAL, match[j].str()));
                        break;
                    case 3:
                        tokens.push_back(Token(EQ, match[j].str()));
                        break;
                }
                break;
            }
        }
    }

    for(auto t : tokens){
        std::cout << t.value << std::endl;
    }

    return "";
}
