#include <expr.hpp>
#include <numeric>
#include <queue>
#include <string>
#include <vector>
#include <regex>
#include <fstream>
#include <iomanip>

std::vector<Expr::Token> Expr::get_tokens(std::string expr){
    std::vector<Token> tokens;
    std::string current_expr = expr;

    while(current_expr.size() > 0){
        bool matched = false;
        for(auto token_pattern : token_patterns){
            std::smatch match;
            if(!std::regex_search(current_expr, match, token_pattern.second, std::regex_constants::match_continuous)) continue;

            if(token_pattern.first != TokenType::SPACE) tokens.push_back(Token(token_pattern.first, match.str()));
            current_expr = match.suffix();
            matched = true;
            break;
        }
        if(!matched) return {};
    }

    return tokens;
}

bool Expr::expr_valid(std::vector<Token> tokens){
    auto is_valid_token = [](TokenType type) {
        switch(type) {
            case TokenType::REGISTER:
            case TokenType::HEX:
            case TokenType::DECIMAL:
            case TokenType::MUL:
            case TokenType::DIV:
            case TokenType::ADD:
            case TokenType::SUB:
            case TokenType::EQ:
                return true;
            default:
                return false;
        }
    };

    std::vector<TokenType> stack;
    for(auto token : tokens){
        if(token.type == TokenType::LPAREN){
            stack.push_back(token.type);
        }
        else if(token.type == TokenType::RPAREN){
            if(stack.empty() || stack.back() != TokenType::LPAREN) return false;
            stack.pop_back();
        }
        else{
            if(stack.empty() || stack.back() == TokenType::LPAREN){
                if(!is_valid_token(token.type)) return false;
            }
            else{
                if(!is_valid_token(token.type)) return false;
            }
        }
    }

    if(!stack.empty()) return false;

    return true;
}

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

std::queue<Expr::Token> Expr::RPN(std::vector<Token> tokens){
    std::stack<Token> Operator;
    std::queue<Token> output;

    for(auto token : tokens){
        switch(token.type){
            case TokenType::REGISTER:
            case TokenType::HEX:
            case TokenType::DECIMAL:
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
    std::vector<Token> tokens = get_tokens(expr);
    if(!expr_valid(tokens)) return "Invalid expression";

    std::queue<Token> Rpn = RPN(tokens);

    if(Rpn.empty()) return "";

    std::vector<uint32_t> stack;
    while(!Rpn.empty()){
        Token token = Rpn.front();
        Rpn.pop();

        switch(token.type){
            case TokenType::REGISTER:
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

    std::stringstream ss;
    ss << std::setfill('0') << std::setw(8) << std::hex << stack.back();
    return "0x" + ss.str();
}

void Expr::test(){
    std::string test_input = "/home/wenjiu/ysyx-workbench/nemu/tools/gen-expr/input";

    std::ifstream input(test_input);
    if(!input.is_open()){
        std::cout << "Cannot open file: " << test_input << std::endl;
        return;
    }

    std::string line;
    while(std::getline(input, line)){
        uint32_t result = std::stoul( line.substr(0, line.find(" ")));
        std::string expr = line.substr(line.find(" ") + 1);

        if(std::stoul(eval(expr), 0, 16) != result){
            std::cout << ANSI_FG_RED << "Test failed: " << ANSI_NONE << expr << std::endl;
            input.close();
            return;
        }

        // std::cout << ANSI_FG_GREEN << "Test passed: " << ANSI_NONE << expr << std::endl;
    }

    input.close();

    std::cout << ANSI_FG_GREEN << "All tests passed" << ANSI_NONE << std::endl;
}
