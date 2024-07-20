// import "DPI-C" function void ram_write(paddr_t addr, int len, word_t data);
import "DPI-C" function int unsigned ram_read (input int unsigned addr, input int len);
import "DPI-C" function void check_special_inst(input int unsigned inst);
import "DPI-C" function void itrace_catch(input int unsigned addr, input int unsigned inst);

module sram_bridge(
    input  clock,
    input  valid,
    input  [31:0] addr,
    output reg [31:0] data
);

    always @(posedge clock) begin
        if (valid) begin
            data = ram_read(addr, 32'd4);
            check_special_inst(data);
            itrace_catch(addr, data);
        end
    end

endmodule