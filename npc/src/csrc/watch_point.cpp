#include <watch_point.hpp>
#include <iomanip>

Watch_Point::Watch_Point(std::string expr, Expr* expr_parser) : expr(expr), expr_parser(expr_parser) {
    if(this->expr_parser->eval(expr) == "Invalid expression") {
        this->value = 0;
        return;
    }

    this->value = std::stoul(expr_parser->eval(expr), 0, 16);
}

bool Watch_Point::check(){
    if(this->value != std::stoul(expr_parser->eval(expr), 0, 16)){
        this->value = std::stoul(expr_parser->eval(expr), 0, 16);
        return true;
    }

    return false;
}

Break_Point::Break_Point(std::string addr_str, Expr* expr_parser) : expr_parser(expr_parser) {
    if(this->expr_parser->eval(addr_str) == "Invalid expression") {
        this->addr = 0;
        return;
    }

    this->addr = std::stoul(expr_parser->eval(addr_str), 0, 16);
}

bool Break_Point::check(){
    return this->addr == std::stoul(this->expr_parser->eval("$pc"), 0, 16);
}

Watch_Point_Manager::Watch_Point_Manager(Expr* expr_parser) : expr_parser(expr_parser) {}

bool Watch_Point_Manager::add_watch_point(std::string expr){
    if(this->expr_parser->eval(expr) == "Invalid expression") return false;

    this->watch_points.emplace_back(expr, this->expr_parser);

    return true;
}

bool Watch_Point_Manager::add_break_point(std::string addr){
    if(this->expr_parser->eval(addr) == "Invalid expression") return false;

    this->break_points.emplace_back(addr, this->expr_parser);

    return true;
}

bool Watch_Point_Manager::delete_watch_point(uint32_t index){
    if(index >= this->watch_points.size()) return false;

    this->watch_points.erase(this->watch_points.begin() + index);

    return true;
}

bool Watch_Point_Manager::delete_break_point(uint32_t index){
    if(index >= this->break_points.size()) return false;

    this->break_points.erase(this->break_points.begin() + index);

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

uint32_t Watch_Point_Manager::check_break_points(){
    uint32_t hit = 0;
    for(auto &bp : this->break_points){
        if(bp.check()){
            std::cout << ANSI_FG_CYAN << "Breakpoint triggered: " << ANSI_NONE << std::hex << "0x" << std::setw(8) << std::setfill('0') << bp.addr << std::endl;
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

void Watch_Point_Manager::print_break_points(){
    for(int i = 0; i < this->break_points.size(); i++){
        std::cout << ANSI_FG_CYAN << "Breakpoint " << i << ANSI_NONE << "\t: " << ANSI_FG_BLUE << this->break_points[i].addr << ANSI_NONE << std::endl;
    }
}
