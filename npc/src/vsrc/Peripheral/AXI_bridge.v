import "DPI-C" function void error_waddr();

module AXI_bridge(
    input clock,
    input [1:0] rresp,
    input [1:0] bresp
);

    always @(posedge clock) begin
        if((rresp == 2'b11) || (bresp == 2'b11)) begin
            error_waddr();
        end
    end

endmodule