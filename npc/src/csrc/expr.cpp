#include <expr.hpp>
#include <numeric>
#include <queue>
#include <string>
#include <vector>
#include <regex>

int Expr::precedence(TokenType type) {
    switch(type) {
        case TokenType::EQ: return 1;
        case TokenType::ADD:
        case TokenType::SUB: return 2;
        case TokenType::MUL:
        case TokenType::DIV: return 3;
        default: return 0;
    }
}

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

std::queue<Expr::Token> Expr::RPN(std::vector<Token> tokens){
    std::stack<Token> Operator;
    std::queue<Token> output;

    for(auto token : tokens){
        switch(token.type){
            case TokenType::DECIMAL:
            case TokenType::HEX:
                output.push(token);
                break;

            case TokenType::EQ:
            case TokenType::ADD:
            case TokenType::SUB:
            case TokenType::MUL:
            case TokenType::DIV:
                while(!Operator.empty() && Operator.top().type != TokenType::LPAREN &&
                      precedence(Operator.top().type) >= precedence(token.type)){
                    output.push(Operator.top());
                    Operator.pop();
                }
                Operator.push(token);
                break;

            case TokenType::LPAREN:
                Operator.push(token);
                break;
            case TokenType::RPAREN:
                while(Operator.top().type != TokenType::LPAREN){
                    output.push(Operator.top());
                    Operator.pop();
                }
                Operator.pop();
                break;

            default: break;
        }
    }

    while(!Operator.empty()){
        output.push(Operator.top());
        Operator.pop();
    }

    return output;
}

std::string Expr::eval(std::string expr){
    std::queue<Token> tokens = RPN(get_tokens(expr));

    std::vector<uint32_t> stack;
    while(!tokens.empty()){
        Token token = tokens.front();
        tokens.pop();

        switch(token.type){
            case TokenType::DECIMAL:
            case TokenType::HEX:
                stack.push_back(token.get_val());
                break;

            case TokenType::EQ:
            case TokenType::ADD:
            case TokenType::SUB:
            case TokenType::MUL:
            case TokenType::DIV:{
                uint32_t b = stack.back();
                stack.pop_back();
                uint32_t a = stack.back();
                stack.pop_back();

                switch(token.type){
                    case TokenType::EQ:
                        stack.push_back(a == b);
                        break;
                    case TokenType::ADD:
                        stack.push_back(a + b);
                        break;
                    case TokenType::SUB:
                        stack.push_back(a - b);
                        break;
                    case TokenType::MUL:
                        stack.push_back(a * b);
                        break;
                    case TokenType::DIV:
                        stack.push_back(a / b);
                        break;
                    default: break;
                }
                break;
            }
            
            default: break;
        }
    }

    return std::to_string(stack.back());
}
