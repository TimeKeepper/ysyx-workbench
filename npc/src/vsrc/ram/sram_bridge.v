import "DPI-C" function void ram_write(paddr_t addr, int len, word_t data);
import "DPI-C" function uint32_t ram_read(uint32_t addr, int len);

module sram_bridge(
    input  clock,
    input  valid,
    input  [31:0] addr,
    output reg [31:0] data,
);

    always @(posedge clock) begin
        if (valid) begin
            data <= ram_read(addr, 4);
        end
    end

endmodule