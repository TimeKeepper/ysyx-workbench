#include <watch_point.hpp>

Watch_Point::Watch_Point(std::string expr, Expr* expr_parser) : expr(expr), expr_parser(expr_parser) {
    if(this->expr_parser->eval(expr) == "Invalid expression") {
        this->value = 0;
        return;
    }

    this->value = std::stoul(expr_parser->eval(expr));
}

bool Watch_Point::check(){
    if(this->value != std::stoul(expr_parser->eval(expr))){
        this->value = std::stoul(expr_parser->eval(expr));
        return true;
    }

    return false;
}

Watch_Point_Manager::Watch_Point_Manager(Expr* expr_parser) : expr_parser(expr_parser) {}

bool Watch_Point_Manager::add_watch_point(std::string expr){
    if(this->expr_parser->eval(expr) == "Invalid expression") return false;

    this->watch_points.emplace_back(expr, this->expr_parser);
    Log("expr %s", expr.c_str());

    return true;
}

bool Watch_Point_Manager::delete_watch_point(uint32_t index){
    if(index >= this->watch_points.size()) return false;

    this->watch_points.erase(this->watch_points.begin() + index);

    return true;
}

uint32_t Watch_Point_Manager::check_watch_points(){
    uint32_t hit = 0;
    for(auto &wp : this->watch_points){
        if(wp.check()){
            std::cout << ANSI_FG_CYAN << "Watchpoint triggered: " << ANSI_NONE << wp.expr << std::endl;
            hit++;
        }
    }

    return hit;
}

void Watch_Point_Manager::print_watch_points(){
    for(int i = 0; i < this->watch_points.size(); i++){
        std::cout << ANSI_FG_CYAN << "Watchpoint " << i << ANSI_NONE << "\t: " << ANSI_FG_BLUE << this->watch_points[i].expr << ANSI_NONE << std::endl;
    }
}
