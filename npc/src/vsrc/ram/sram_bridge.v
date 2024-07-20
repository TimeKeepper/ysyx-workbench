// import "DPI-C" function void ram_write(paddr_t addr, int len, word_t data);
import "DPI-C" function int unsigned ram_read (input int unsigned addr, input int len);
import "DPI-C" function void ram_write (input int unsigned addr, input int len, input int unsigned data);

module sram_bridge(
    input  clock,
    input  read,
    input  [31:0] r_addr,
    output reg [31:0] r_data,
    input  write,
    input  [31:0] w_addr,
    input  [31:0] w_data,
    input  [3:0]  w_strb
);

    always @(posedge clock) begin
        if (read) begin
            r_data <= ram_read(r_addr, 32'd4);
        end
        if (write) begin
            if(w_strb == 4'b0001) begin
                ram_write(w_addr, 32'd1, w_data);
            end else if(w_strb == 4'b0011) begin
                ram_write(w_addr, 32'd2, w_data);
            end else if(w_strb == 4'b1111) begin
                ram_write(w_addr, 32'd4, w_data);
            end
        end
    end

endmodule