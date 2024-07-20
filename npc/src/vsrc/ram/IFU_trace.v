import "DPI-C" function void check_special_inst(input int unsigned inst);
import "DPI-C" function void itrace_catch(input int unsigned addr, input int unsigned inst);

module IFU_trace(
    input clock,
    input valid,
    input [31:0] addr,
    input [31:0] data
);

    always @(posedge clock) begin
        if(valid) begin
            check_special_inst(data);
            itrace_catch(addr, data);
        end
    end

endmodule