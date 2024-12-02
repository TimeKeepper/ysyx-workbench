#include <exprtk.hpp>

std::string expr(std::string expr){    
    typedef exprtk::symbol_table<double> symbol_table_t;
    typedef exprtk::expression<double> expression_t;
    typedef exprtk::parser<double> parser_t;
    
    symbol_table_t symbol_table;
    expression_t expression;
    parser_t parser;
    
    expression.register_symbol_table(symbol_table);
    
    if (!parser.compile(expr, expression)) {
        return "Error: " + std::string(parser.error().c_str());
    }
    
    uint32_t result = expression.value();
    return std::to_string(result);
}
